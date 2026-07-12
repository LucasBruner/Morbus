# Arquitetura do Sistema — Fila SUS
> Hackathon FIAP PosTech — Arquitetura e Desenvolvimento Java — Fase 5

---

## 1. Visão Geral

O sistema digitaliza o processo de **regulação e agendamento ambulatorial do SUS (SISREG)**, cobrindo o ciclo completo: desde a solicitação de um procedimento pela UBS até o agendamento confirmado com data, hora e local na unidade executante.

O sistema é composto por **cinco microsserviços** independentes que se comunicam via REST/GraphQL (operações síncronas) e RabbitMQ (eventos assíncronos).

```
                         ┌─────────────────────────────────────────────────────────────────┐
                         │                         auth-service :8082                      │
                         │          (Spring Boot MVC — emissão de JWT)                     │
                         └───────────────────────────┬─────────────────────────────────────┘
                                    JWT compartilhado │ (JWT_SECRET)
       ┌────────────────────────────┬────────────────┼───────────────┬────────────────────────┐
       ▼                            ▼                ▼               ▼                        ▼
┌─────────────────┐   ┌─────────────────┐   ┌──────────────┐   ┌───────────────────┐   ┌──────────────────────┐
│ regulacao-      │   │  queue-service  │   │ agendamento- │   │  notification-    │   │      Cliente         │
│ service :8083   │   │  :8080          │   │ service :8084│   │  service :8081    │   │  (Postman/Swagger/   │
│                 │   │                 │   │              │   │                   │   │   Frontend)          │
│ Hexagonal       │   │ Clean           │   │ CQRS +       │   │ Event-driven      │   └──────────────────────┘
│ Architecture    │   │ Architecture    │   │ GraphQL      │   │ (Quarkus)         │
│ REST            │   │ REST            │   │ REST+GraphQL │   │                   │
└────────┬────────┘   └────────┬────────┘   └──────┬───────┘   └───────────────────┘
         │ PostgreSQL          │ PostgreSQL          │ PostgreSQL
         ▼                     ▼                     ▼
   [regulacao_db]        [queue_db]            [agendamento_db]

──────────────────────────────── RabbitMQ :5672 ────────────────────────────────
  sus.regulacao.exchange ──▶ queue-service, notification-service
  sus.queue.exchange     ──▶ agendamento-service, notification-service
  sus.agendamento.exchange──▶ queue-service, notification-service
```

**Fluxo de negócio ponta a ponta:**
```
[UBS / SOLICITANTE]
      │  POST /api/v1/solicitacoes
      ▼
[regulacao-service]  ─── REGULADOR avalia e aprova/nega ───▶ SOLICITATION_APPROVED
      │ (evento RabbitMQ)
      ▼
[queue-service]  ─── fila priorizada por tipoFila + riskColor + grupo + chegada ───▶ PATIENT_CALLED
      │ (evento RabbitMQ)
      ▼
[agendamento-service]  ─── aloca slot, confirma ou devolve ───▶ APPOINTMENT_CONFIRMED
      │ (eventos RabbitMQ)
      ▼
[notification-service]  ─── e-mail/notificação em cada etapa ───▶ [Paciente / UBS]
```

---

## 2. Serviços

### 2.1 auth-service

**Responsabilidade:** Autenticar usuários e emitir tokens JWT com a role do perfil.

| Atributo       | Valor                                   |
|----------------|-----------------------------------------|
| Framework      | Spring Boot 4 + Spring Security         |
| Padrão         | MVC                                     |
| Porta          | `8082`                                  |
| Banco de dados | PostgreSQL (schema `auth`)              |
| Autenticação   | JWT assinado com HMAC-SHA256            |

**Estrutura de pacotes:**
```
auth-service/
└── src/main/java/br.com.morbus.authservice/
    ├── config/       # SecurityConfig
    ├── controller/   # AuthController
    ├── exception/    # GlobalExceptionHandler, InvalidCredentialsException,
    │                 # PasswordNotValidException, UserAlreadyExistException, UserOrPasswordIncorrect
    ├── model/        # User.java, UserRole.java (enum, sem prefixo ROLE_)
    │   └── dto/      # NewUserDTO, LoginRequestDTO, AuthResponseDTO, UserPresenterDTO
    ├── repository/   # UserRepository
    └── service/      # AuthService, JwtService
```
> Não há `JwtAuthFilter` neste serviço: o auth-service só emite tokens (endpoints `permitAll`) e não valida JWT de terceiros — a validação roda nos demais serviços (seção 6). Também não expõe Swagger/OpenAPI (sem dependência SpringDoc).

**Endpoints expostos:**

| Método | Path           | Auth | Descrição                        |
|--------|----------------|------|----------------------------------|
| POST   | /auth/register | ❌   | Cria novo usuário com role       |
| POST   | /auth/login    | ❌   | Valida credenciais e retorna JWT |

**Roles disponíveis:**

| Role               | Descrição                                                    |
|--------------------|--------------------------------------------------------------|
| `ROLE_MEDICO`      | Profissional de saúde — gerencia a fila completa             |
| `ROLE_PACIENTE`    | Paciente — consulta posição e confirma presença              |
| `ROLE_SOLICITANTE` | Operador de UBS — cria e complementa solicitações            |
| `ROLE_REGULADOR`   | Médico regulador — avalia solicitações e emite pareceres     |
| `ROLE_EXECUTANTE`  | Responsável da unidade executante — gerencia grade de slots  |

---

### 2.2 queue-service

**Responsabilidade:** Núcleo do sistema — gerencia a fila priorizada de pacientes, com suporte a dois tipos de fila (FILA_ESPERA e FILA_REGULADA), controle de cotas e publicação de eventos.

| Atributo       | Valor                                               |
|----------------|-----------------------------------------------------|
| Framework      | Spring Boot 4.0.6 + Spring Data JPA + Spring AMQP  |
| Padrão         | **Clean Architecture**                              |
| Porta          | `8080`                                              |
| Banco de dados | PostgreSQL (schema `queue`)                         |
| Migrations     | Flyway                                              |
| Docs           | SpringDoc OpenAPI (Swagger UI em `/swagger-ui.html`)|

**Estrutura de pacotes (Clean Architecture):**
```
queue-service/
└── src/main/java/br.com.morbus.queueservice/
    ├── domain/
    │   ├── entity/       # Patient, QueueEntry, Procedure, UnitProcedureQuota
    │   ├── enums/        # ERiskColor, EPriorityGroup, EQueueStatus, EGender, EDestino
    │   ├── event/        # IQueueEventPublisher (contrato de saída)
    │   ├── exception/    # QueueNotExistException, QueueNotAllowedException,
    │   │                 # QueueEmptyException, QuotaExceededException, QuotaNotFoundException (17 no total)
    │   ├── repository/   # IQueueEntryRepository, IPatientRepository, IProcedureRepository,
    │   │                 # IUnitProcedureQuotaRepository, IPatientProcedureRepository
    │   ├── service/      # PriorityCalculator
    │   └── usecase/      # CallNextPatient, ReclassifyPriority, AddToQueue, CancelQueueEntry,
    │       │             # CheckAndEnforceQuota, CreateOrUpdateQuota, GetQuota, ReinstatePatientInQueue...
    │       └── dto/      # DTOs de entrada/saída dos usecases
    └── infrastructure/
        ├── config/       # RabbitMQConfig, UseCaseConfig, OpenApiConfig
        ├── database/
        │   ├── entity/       # PatientEntity, QueueEntryEntity, ProcedureEntity, UnitProcedureQuotaEntity
        │   ├── persistence/  # PatientRepositoryImpl, QueueEntryRepositoryImpl, ProcedureRepositoryImpl,
        │   │                 # UnitProcedureQuotaRepositoryImpl
        │   └── repository/   # PatientJpaRepository, QueueEntryJpaRepository, ProcedureJpaRepository,
        │                     # UnitProcedureQuotaJpaRepository
        ├── http/
        │   └── controller/   # QueueController, PatientController, ProcedureController, QuotaController
        ├── messaging/    # RabbitMqQueueEventPublisher
        │   ├── consumer/ # SolicitationApprovedConsumer, AppointmentConfirmedConsumer,
        │   │             # AppointmentExpiredConsumer, PatientNoShowConsumer,
        │   │             # AppointmentCancelledConsumer, AppointmentNoSlotConsumer
        │   └── DTO/      # QueueEventPayload e eventos de entrada de cada consumer
        └── security/     # JwtAuthenticationFilter (SecurityConfig fica em infrastructure/config/)
```

**Regra de dependências:**
```
interfaces → application → domain ← infrastructure
```
Nenhuma classe do `domain` importa Spring, JPA ou RabbitMQ.

**Endpoints expostos:**

| Método | Path                                        | Role             | Descrição                          |
|--------|---------------------------------------------|------------------|------------------------------------|
| POST   | /api/v1/queue                               | MEDICO           | Cadastra paciente na fila          |
| GET    | /api/v1/queue?procedureId=                  | MEDICO           | Lista fila ordenada por prioridade |
| GET    | /api/v1/queue/{id}/position                 | MEDICO, PACIENTE | Posição do paciente na fila        |
| POST   | /api/v1/queue/call-next                     | MEDICO           | Chama o próximo da fila            |
| PATCH  | /api/v1/queue/{id}/priority                 | MEDICO           | Reclassifica cor de risco          |
| DELETE | /api/v1/queue/{id}                          | MEDICO           | Cancela entrada na fila            |
| POST   | /api/v1/patients                            | MEDICO           | Cadastra paciente                  |
| GET    | /api/v1/patients/{id}                       | MEDICO           | Busca paciente por ID              |
| GET    | /api/v1/patients?cpf=                       | MEDICO           | Busca paciente por CPF             |
| PATCH  | /api/v1/patients/{id}                       | MEDICO           | Atualiza dados do paciente         |
| PATCH  | /api/v1/patients/{id}/inactivate            | MEDICO           | Inativa paciente                   |
| POST   | /api/v1/patients/{pId}/procedures/{procId}  | MEDICO           | Vincula procedimento ao paciente   |
| DELETE | /api/v1/patients/{pId}/procedures/{procId}  | MEDICO           | Desvincula procedimento do paciente|
| GET    | /api/v1/procedures                          | MEDICO, PACIENTE | Lista procedimentos disponíveis    |
| GET    | /api/v1/procedures?codigo=                  | MEDICO, PACIENTE | Busca procedimento por código SIGTAP|
| GET    | /api/v1/procedures/{id}                     | MEDICO, PACIENTE | Busca procedimento por ID          |
| POST   | /api/v1/quotas                              | MEDICO           | Cria/atualiza cota diária de FILA_ESPERA para unidade+procedimento |
| GET    | /api/v1/quotas?unitId=&procedureId=         | MEDICO           | Consulta a cota configurada         |

**Tipos de fila (EDestino):**

| Tipo            | Cor de risco     | Algoritmo de ordenação                  | Controle de cota |
|-----------------|------------------|-----------------------------------------|------------------|
| `FILA_ESPERA`   | Sempre `AZUL`    | Cronológico puro (registeredAt ASC)     | Sim — por unidade (`preferredUnitId`) + procedimento, opt-in |
| `FILA_REGULADA` | Definido pelo regulador | riskColor → priorityGroup → registeredAt | Não         |

> FILA_REGULADA sempre precede FILA_ESPERA na ordenação global — aplicado tanto na query de listagem (`GET /api/v1/queue`) quanto no cálculo de posição.

**Controle de cota (UnitProcedureQuota):**

Cota diária, opt-in, por combinação `unitId` (a `preferredUnitId` do `QueueEntry`) + `procedureId`. Se não houver uma linha configurada para a combinação, não há limite. Quando configurada (`maxPerDay`), o `CheckAndEnforceQuota` conta as entradas `AGUARDANDO` em `FILA_ESPERA` daquela unidade+procedimento no dia corrente e lança `QuotaExceededException` (409) se o limite já foi atingido. FILA_REGULADA nunca é afetada pela cota.

---

### 2.3 notification-service

**Responsabilidade:** Consumir eventos de todos os serviços e enviar notificações ao paciente e/ou à UBS.

| Atributo       | Valor                                                     |
|----------------|-----------------------------------------------------------|
| Framework      | Quarkus + SmallRye Reactive Messaging + Hibernate Panache |
| Padrão         | **Event-driven MVC**                                      |
| Porta          | `8081`                                                    |
| Banco de dados | PostgreSQL (schema `notification`)                        |
| Docs           | Quarkus OpenAPI (`/q/swagger-ui`)                         |

**Estrutura de pacotes:**
```
notification-service/
└── src/main/java/br.com.sus.notificationservice/
    ├── config/      # OpenApiConfig
    ├── consumer/    # QueueEventConsumer, AppointmentEventConsumer, SolicitacaoEventConsumer,
    │                # NotificationEventConverter
    ├── controller/  # NotificationController
    ├── model/       # Notification.java (@Entity Panache, tabela `notifications` no schema `notification`)
    │   └── dto/     # QueueEventDTO, NotificationQueueDTO + um DTO por tipo de evento de
    │                # agendamento/regulação (AppointmentConfirmedEventDTO, AppointmentNoSlotEventDTO,
    │                # AppointmentRescheduledEventDTO, AppointmentCancelledEventDTO, AppointmentExpiredEventDTO,
    │                # SolicitacaoNegadaEventDTO, SolicitacaoDevolvidaEventDTO)
    ├── repository/  # NotificationRepository (PanacheRepository)
    └── service/     # NotificationService, EmailService
```
> `EmailService` hoje apenas simula o envio (log `[EMAIL SIMULADO]`), sem integração SMTP real.
> Bindings do RabbitMQ ficam em `application.properties` (`mp.messaging.incoming.*`), não em uma classe `@ApplicationScoped` de config dedicada.

**Eventos consumidos** — uma fila dedicada por tipo de evento (não uma fila genérica por serviço de origem):

| Fila RabbitMQ                        | Exchange origem          | Evento                  | Destinatário       |
|---------------------------------------|--------------------------|-------------------------|--------------------|
| notification.queue.events (routing keys: patient.registered, patient.called, priority.updated, patient.cancelled, patient.reinstated) | sus.queue.exchange | PATIENT_REGISTERED, PATIENT_CALLED, PRIORITY_UPDATED, PATIENT_CANCELLED, PATIENT_REINSTATED | Paciente |
| notification.appointment.confirmed    | sus.agendamento.exchange | APPOINTMENT_CONFIRMED   | Paciente           |
| notification.appointment.cancelled    | sus.agendamento.exchange | APPOINTMENT_CANCELLED   | Paciente           |
| notification.appointment.rescheduled  | sus.agendamento.exchange | APPOINTMENT_RESCHEDULED | Paciente           |
| notification.appointment.no_slot      | sus.agendamento.exchange | APPOINTMENT_NO_SLOT     | Paciente           |
| notification.appointment.expired      | sus.agendamento.exchange | APPOINTMENT_EXPIRED     | Paciente           |
| notification.solicitation.denied      | sus.regulacao.exchange   | SOLICITATION_DENIED     | UBS Solicitante    |
| notification.solicitation.devolved    | sus.regulacao.exchange   | SOLICITATION_DEVOLVED   | UBS Solicitante    |

---

### 2.4 regulacao-service

**Responsabilidade:** Upstream do queue-service. Gerencia o ciclo de vida de solicitações ambulatoriais — desde a criação pela UBS até a decisão do médico regulador, determinando se o paciente entra na fila e com qual classificação de risco.

| Atributo       | Valor                                               |
|----------------|-----------------------------------------------------|
| Framework      | Spring Boot 4 + Spring Data JPA + Spring AMQP       |
| Padrão         | **Hexagonal Architecture (Ports & Adapters)**       |
| Porta          | `8083`                                              |
| Banco de dados | PostgreSQL (schema `regulacao`)                     |
| Migrations     | Flyway                                              |
| Docs           | SpringDoc OpenAPI (Swagger UI em `/swagger-ui.html`)|
| API            | REST                                                |

**Estrutura de pacotes (Hexagonal):**
```
regulacao-service/
└── src/main/java/br.com.morbus.regulacao/
    ├── domain/
    │   ├── model/        # Solicitacao, Parecer, UnidadeSolicitante, Quota
    │   ├── enums/        # EStatusSolicitacao, EDecisaoRegulador, EDestino, ERiscoSolicitado
    │   ├── dto/           # PageResult<T>, ESortDirection — tipos de paginação/ordenação
    │   │                  # próprios do domínio (não usam Page/Sort do Spring Data)
    │   ├── exception/    # SolicitacaoNaoEncontradaException, CotaExcedidaException,
    │   │                 # DuplicateSolicitacaoException, CampoObrigatorioException,
    │   │                 # IdPacienteIncorretoException, SolicitacaoNaoPendenteException,
    │   │                 # UnidadeSolicitanteDuplicadaException, UnidadeSolicitanteNaoEncontradaException
    │   └── usecase/      # implementações dos ports/in, organizadas em subpacotes:
    │                     # solicitacao/ (CriarSolicitacaoUseCase, AvaliarSolicitacaoUseCase,
    │                     #   ComplementarSolicitacaoUseCase, ReclassificarRiscoUseCase,
    │                     #   TransicionarParaAgendada/Atendida/FaltouUseCase, CancelarSolicitacaoUseCase, ...),
    │                     # quota/ (ConsultarCotasUseCase, GerenciarCotaUseCase),
    │                     # unidade/ (CadastrarUnidadeSolicitanteUseCase, BuscarUnidadeSolicitanteUseCase)
    ├── ports/
    │   ├── in/           # I-prefixed: ICriarSolicitacaoUseCase, IAvaliarSolicitacaoUseCase,
    │   │                 # IComplementarSolicitacaoUseCase, IListarSolicitacoesUseCase, ...
    │   └── out/          # ISolicitacaoRepository, IParecerRepository, IQuotaRepository,
    │                     # IUnidadeSolicitanteRepository, IRegulacaoEventPublisher
    └── adapters/
        ├── in/
        │   ├── rest/     # SolicitacaoController, RegulacaoController, CotaController,
        │   │             # UnidadeSolicitanteController, GlobalExceptionHandler, PageMapper, DTOs
        │   └── AppointmentCreatedConsumer, AppointmentAttendedConsumer, AppointmentNoShowConsumer
        ├── out/
        │   ├── jpa/      # entidades JPA, Spring Data repositories, adapters de ISolicitacaoRepository etc.
        │   ├── rabbitmq/ # RabbitMQConfig, RabbitMqRegulacaoEventPublisher, DTOs de payload
        │   ├── config/   # OpenApiConfig, UseCaseConfig
        │   └── security/ # JwtAuthenticationFilter, JwtService, SecurityConfig — apesar do nome "out", faz a validação de entrada
        └── security/     # UserPrincipal — pacote irmão de in/out, não aninhado em nenhum dos dois
```
> A fronteira transacional (`@Transactional`) vive nos adapters de entrada (controllers REST e consumers RabbitMQ), não no domínio.

**Regra de dependências (Hexagonal):**
```
adapters/in ──▶ ports/in ──▶ domain ◀── ports/out ◀── adapters/out
```
O domínio não conhece Spring, JPA, RabbitMQ nem os adapters — inclusive a paginação/ordenação usa tipos próprios (`PageResult`, `ESortDirection`) em vez de `Page`/`Sort` do Spring Data.

**Decisões do regulador (EDecisaoRegulador):**

| Decisão       | Efeito                                                              |
|---------------|---------------------------------------------------------------------|
| `AUTORIZAR`   | Entra em FILA_REGULADA com riskColor definido pelo regulador        |
| `FILA_ESPERA` | Entra em FILA_ESPERA com riskColor fixo AZUL                        |
| `NEGAR`       | Solicitação negada — notifica UBS com justificativa                 |
| `DEVOLVER`    | Dados incompletos — UBS deve complementar e reenviar               |
| `PENDENTE`    | Aprovado mas sem vaga disponível — aguarda abertura de cota         |

**Endpoints expostos:**

| Método | Path                                        | Role                    | Descrição                                   |
|--------|----------------------------------------------|-------------------------|---------------------------------------------|
| POST   | /api/v1/solicitacoes                        | SOLICITANTE             | Cria nova solicitação                       |
| GET    | /api/v1/solicitacoes                        | SOLICITANTE / REGULADOR | Lista solicitações (filtros)                |
| GET    | /api/v1/solicitacoes/{id}                   | SOLICITANTE / REGULADOR | Detalhe + histórico de pareceres            |
| DELETE | /api/v1/solicitacoes/{id}                   | MEDICO / SOLICITANTE    | Cancela solicitação                         |
| POST   | /api/v1/solicitacoes/{id}/complementar      | SOLICITANTE             | Complementa solicitação devolvida           |
| POST   | /api/v1/regulacao/{id}/avaliar              | REGULADOR               | Emite parecer (decisão + riskColor)         |
| GET    | /api/v1/regulacao/pendentes                 | REGULADOR               | Lista solicitações aguardando avaliação     |
| GET    | /api/v1/regulacao/pendentes-vaga            | REGULADOR               | Lista aprovadas sem vaga disponível         |
| PATCH  | /api/v1/regulacao/solicitacoes/{id}/risco   | REGULADOR               | Reclassifica a cor de risco de uma solicitação |
| POST   | /api/v1/regulacao/cotas                     | REGULADOR               | Cria/atualiza cota de uma unidade+procedimento |
| GET    | /api/v1/regulacao/cotas                     | REGULADOR               | Lista cotas cadastradas                     |
| POST   | /api/v1/unidades-solicitantes               | REGULADOR               | Cadastra unidade solicitante (UBS)          |
| GET    | /api/v1/unidades-solicitantes/{id}          | REGULADOR / SOLICITANTE | Busca unidade solicitante por ID            |

**Eventos consumidos:** `AppointmentCreatedConsumer`, `AppointmentAttendedConsumer` e `AppointmentNoShowConsumer` consomem `appointment.created`, `appointment.attended` e `appointment.no_show` de `sus.agendamento.exchange` (com DLX dedicada) — o regulacao-service não é apenas publicador, também reage ao ciclo de vida do agendamento. Esses consumers **não são apenas estatísticos**: chamam `ITransicionarParaAgendadaUseCase`/`ITransicionarParaAtendidaUseCase`/`ITransicionarParaFaltouUseCase`, que efetivamente movem a `Solicitacao` por `APROVADA → AGENDADA → ATENDIDA`/`FALTOU` (ver `requisitos-dominio-sus.md` §6 e `erd.md`).

**Eventos publicados em `sus.regulacao.exchange`:** além de `solicitation.approved`/`denied`/`devolved`, também publica `solicitation.reclassified` quando a cor de risco de uma solicitação já aprovada é alterada.

> ⚠️ **Duas cotas distintas no sistema, não confundir:** o regulacao-service tem seu próprio conceito de `Quota` (`maxPerPeriod`/`currentCount`/`periodStart` por unidade+procedimento, gerenciável via `CotaController`). Ela é verificada/incrementada em `CriarSolicitacaoUseCase` quando `destino = FILA_ESPERA`, e também em `AvaliarSolicitacaoUseCase` quando o regulador decide `FILA_ESPERA` para uma solicitação que não nasceu com esse destino (`CotaExcedidaException` bloqueia a aprovação nesse caso). Já a `UnitProcedureQuota` do **queue-service** (seção 2.2) é um mecanismo diferente e independente: um limite diário, opt-in e automaticamente enforçado, exclusivo de `FILA_ESPERA`.

---

### 2.5 agendamento-service

**Responsabilidade:** Downstream do queue-service. Gerencia a grade de horários das unidades executantes e aloca slots de data/hora/local assim que um paciente é chamado da fila.

| Atributo       | Valor                                               |
|----------------|-----------------------------------------------------|
| Framework      | Spring Boot 4 + Spring Data JPA + Spring AMQP       |
| Padrão         | **CQRS (Command Query Responsibility Segregation)** |
| Porta          | `8084`                                              |
| Banco de dados | PostgreSQL (schema `agendamento`)                   |
| Migrations     | Flyway                                              |
| Docs           | SpringDoc OpenAPI + GraphiQL (`/graphiql`) — **GraphiQL só habilitado no profile `local`**, desabilitado em `docker`/produção |
| API            | REST (commands) + GraphQL (queries)                 |
| Scheduler      | Spring `@Scheduled` (a cada 15 min) — expira agendamentos vencidos após `AGENDAMENTO_EXPIRACAO_HORAS` |

**Estrutura de pacotes (CQRS):**
```
agendamento-service/
└── src/main/java/br.com.morbus.agendamento/
    ├── domain/
    │   ├── model/        # Schedule, Slot, Agendamento, HealthUnit, Provider, AgendamentoComDetalhes
    │   ├── enums/        # EDiaSemana, EStatusSlots, EStatusAgendamento
    │   ├── exception/    # 10 exceções (DuplicateScheduleException, InvalidSchedulePeriodException,
    │   │                 # DuplicateAgendamentoException, CancelamentoNaoPermitidoException, ...)
    │   └── port/
    │       ├── in/       # 15 use case ports — ex.: IDetalharAgendamentoUseCase, IAgendamentosPacienteUseCase
    │       │             # (recebem UUID/String primitivos, nunca Authentication ou DTOs de adapter)
    │       └── out/      # portas de saída (persistência, eventos)
    ├── application/
    │   ├── command/      # CriarScheduleCommand, CriarScheduleResult, CriarAgendamentoCommand
    │   └── usecase/      # 16 use cases — CriarScheduleUseCase, CriarAgendamentoUseCase,
    │                     # CancelarAgendamentoUseCase, ExpirarAgendamentosUseCase, DetalharAgendamentoUseCase, ...
    ├── adapter/
    │   ├── in/
    │   │   ├── rest/     # ScheduleController, AppointmentController
    │   │   │   └── dto/  # ScheduleRequestDTO, ScheduleCreatedResponseDTO
    │   │   ├── graphql/  # AgendamentoGraphqlController + DTOs (traduz Authentication/request GraphQL
    │   │   │             # em UUID/String antes de chamar os use cases — a tradução fica no adapter, não no domínio)
    │   │   └── rabbitmq/ # PatientCalledConsumer, PatientCalledEvent
    │   ├── out/
    │   │   ├── persistence/ # ScheduleEntity, SlotEntity, AgendamentoEntity, HealthUnitEntity, ProviderEntity,
    │   │   │                # repositórios JPA e adapters de persistência
    │   │   ├── rabbitmq/    # RabbitMQConfig, AgendamentoEventPublisher, IAgendamentoEventPublisher,
    │   │   │                # eventos (AppointmentConfirmedEvent, AppointmentCancelledEvent, AppointmentExpiredEvent, ...)
    │   │   └── security/    # JwtAuthenticationFilter, JwtService, SecurityConfig
    │   └── security/     # UserPrincipal
    └── infrastructure/
        ├── config/       # UseCaseConfig, SchedulerConfig
        └── scheduling/   # AppointmentExpirationJob
```

**Separação CQRS:**

| Lado    | Operação                                | Canal |
|---------|-----------------------------------------|-------|
| Command | AlocarSlot, Cancelar, Reagendar, Falta  | REST  |
| Command | VerificarExpiracoes (`AGENDAMENTO_EXPIRACAO_HORAS`, checado a cada 15 min) | `@Scheduled` |
| Query   | Disponibilidade de slots                | GraphQL |
| Query   | Agendamentos do paciente/unidade        | GraphQL |
| Query   | Grade semanal da unidade                | GraphQL |

**Schema GraphQL (consultas disponíveis — nomes de campo reais, note a mistura pt-BR/inglês):**
```graphql
type Query {
  disponibilidade(
    procedureId: ID!
    unitId: ID
    dataInicio: String!
    dataFim: String!
  ): [Slot!]!

  agendamentos(
    pacienteId: ID
    unitId: ID
    status: AppointmentStatus
    dateFrom: String!
    dateTo: String!
  ): [Appointment!]!

  grade(unitId: ID!, week: String!): [Schedule!]!
  agendamento(id: ID!): Appointment
}
```

> `AgendamentoGraphqlController` usa `@Argument` com nome explícito (`@Argument("dataInicio")`, `@Argument("pacienteId")`) nos dois pontos onde o schema usa um nome em português diferente do parâmetro Java interno — sem isso, o binding por nome de parâmetro divergia do schema e os filtros de data/paciente ficavam silenciosamente `null`.

`HealthUnit.address` é montado no resolver da query `grade` como `"{municipio} - {uf}"` (não é a coluna `endereco`); `HealthUnit.phone` reflete a coluna `telefone` (V10). Ver `erd.md`.

**Endpoints REST (commands):**

| Método | Path                                 | Role       | Descrição                        |
|--------|--------------------------------------|------------|----------------------------------|
| PATCH  | /api/v1/appointments/{id}/confirmar  | PACIENTE   | Confirma presença (prazo 72h)    |
| PATCH  | /api/v1/appointments/{id}/attend     | EXECUTANTE | Marca agendamento como atendido  |
| DELETE | /api/v1/appointments/{id}            | PACIENTE / MEDICO | Cancela agendamento       |
| PATCH  | /api/v1/appointments/{id}/reagendar  | MEDICO     | Reagenda para outro slot         |
| POST   | /api/v1/appointments/{id}/falta      | EXECUTANTE | Registra falta do paciente       |
| POST   | /api/v1/schedules                    | EXECUTANTE | Cria grade semanal               |
| PUT    | /api/v1/schedules/{id}               | EXECUTANTE | Atualiza grade                   |
| POST   | /api/v1/schedules/{id}/block         | EXECUTANTE | Bloqueia slots (feriado etc.)    |
| POST   | /api/v1/schedules/{id}/unblock       | EXECUTANTE | Desbloqueia slots da grade       |

> `DELETE /api/v1/appointments/{id}` publica `appointment.cancelled` em `sus.agendamento.exchange` (routing key já existia na topologia, mas não era usada até esta correção).

---

## 3. Comunicação entre Serviços

### 3.1 Autenticação (síncrona — REST/JWT)

O `auth-service` emite o JWT. Todos os demais serviços validam o token localmente com o `JWT_SECRET` compartilhado, sem chamadas ao auth-service em runtime.

```
Cliente ──POST /auth/login──▶ auth-service
         ◀── { token: "eyJ..." } ──

Cliente ──Request (Bearer eyJ...)──▶ queue/regulacao/agendamento-service
                                     JwtAuthenticationFilter valida localmente
                                     extrai role → SecurityContext
```
> O auth-service não tem (nem precisa de) filtro de validação — só emite tokens.

### 3.2 Topologia RabbitMQ completa

Cada serviço consumidor declara sua **própria fila**, ligada à mesma routing key — não existe uma fila única compartilhada entre serviços diferentes.

```
sus.regulacao.exchange (direct)        publicado por: regulacao-service
├── solicitation.approved   →  queue-service: "queue.solicitation.approved"
│                              (notification-service NÃO consome este evento)
├── solicitation.denied     →  notification-service: "notification.solicitation.denied"
├── solicitation.devolved   →  notification-service: "notification.solicitation.devolved"
└── solicitation.reclassified → sem consumidor ainda (publicado para uso futuro)

sus.queue.exchange (direct)            publicado por: queue-service
├── patient.registered, patient.called, priority.updated,
│   patient.cancelled, patient.reinstated
│      →  notification-service: fila única "notification.queue.events"
│         (uma fila com múltiplas routing keys, ao contrário do padrão 1 fila = 1 evento dos demais exchanges)
└── patient.called →  agendamento-service: "queue.patient.called" (PatientCalledConsumer)

sus.agendamento.exchange (direct)      publicado por: agendamento-service
├── appointment.confirmed   →  queue-service: "queue.appointment.confirmed" (ConfirmAppointment — marca AGENDADO)
│                              notification-service: "notification.appointment.confirmed"
├── appointment.cancelled   →  queue-service: "queue.appointment.cancelled" (reinstate)
│                              notification-service: "notification.appointment.cancelled"
├── appointment.rescheduled →  notification-service: "notification.appointment.rescheduled"
├── appointment.no_slot     →  queue-service: "queue.appointment.no_slot" (reinstate)
│                              notification-service: "notification.appointment.no_slot"
├── appointment.expired     →  queue-service: "queue.appointment.expired" (reinstate)
│                              notification-service: "notification.appointment.expired"
├── appointment.attended    →  regulacao-service: "regulacao.appointment.attended"
├── appointment.created     →  regulacao-service: "regulacao.appointment.created"
└── patient.no_show         →  queue-service: "queue.patient.no_show" (reinstate)
                               regulacao-service: "regulacao.appointment.no_show"
```
> `appointment.cancelled` e `appointment.no_slot` reinserem o paciente na fila via `ReinstatePatientInQueue` no queue-service (mesma semântica já usada por `appointment.expired`/`patient.no_show`).

**Dead Letter Exchange (DLX):**

Cada consumer do `queue-service` (`SolicitationApprovedConsumer`, `AppointmentConfirmedConsumer`, `AppointmentExpiredConsumer`, `PatientNoShowConsumer`, `AppointmentCancelledConsumer`, `AppointmentNoSlotConsumer`) tem sua fila principal ligada a um exchange de dead-letter dedicado, `sus.queue.dlx` (`RabbitMQConfig.java`). O regulacao-service segue o mesmo padrão para seus consumers de agendamento, com DLQs próprias. Mensagens que falham repetidamente no processamento (após as tentativas de retry do `spring.rabbitmq.listener.simple.retry.*`) são roteadas para uma fila `.dlq` correspondente (ex.: `queue.solicitation.approved.dlq`) em vez de serem perdidas ou reentregues indefinidamente — permitindo inspeção/reprocessamento manual via RabbitMQ Management.

**Payload base dos eventos (JSON) — `QueueEventPayload`:**
```json
{
  "eventType": "PATIENT_CALLED",
  "queueEntryId": "uuid",
  "patientId": "uuid",
  "patientName": "João da Silva",
  "patientContact": "joao@email.com",
  "procedureName": "Consulta de Cardiologia",
  "procedureId": "uuid",
  "preferredUnitId": "uuid",
  "riskColor": "AMARELO",
  "tipoFila": "FILA_REGULADA",
  "motivoCancelamento": null,
  "timestamp": "2026-06-12T10:30:00Z"
}
```
> `patientId`, `procedureId`, `preferredUnitId` e `tipoFila` são obrigatórios para o `PatientCalledConsumer`/`AlocarPacienteEmSlotUseCase` do agendamento-service localizar um slot e criar o `Agendamento` a partir do evento `patient.called`. `motivoCancelamento` só é preenchido em `PATIENT_CANCELLED`; nos demais eventos vai `null`.

---

## 4. Banco de Dados

Cada serviço possui seu próprio schema PostgreSQL. Não existem foreign keys entre schemas — a consistência entre serviços é garantida por eventos.

> `auth-service`, `queue-service` e `notification-service` compartilham o mesmo banco físico (`sus_queue_db`) via schemas distintos (`auth`, `queue`, `notification`); `regulacao-service` e `agendamento-service` têm bancos próprios (`regulacao_db`, `agendamento_db`). Todos os schemas/bancos são criados por `init.sql` no primeiro start do container `postgres`.

| Schema          | Serviço               | Tabelas principais                                             |
|-----------------|-----------------------|----------------------------------------------------------------|
| `auth`          | auth-service          | `users`                                                        |
| `queue`         | queue-service         | `patients`, `procedures`, `queue_entries`, `unit_procedure_quotas` |
| `notification`  | notification-service  | `notifications`                                                |
| `regulacao`     | regulacao-service     | `solicitacoes`, `pareceres`, `unidades_solicitantes`, `quotas` |
| `agendamento`   | agendamento-service   | `health_units`, `providers`, `schedules`, `slots`, `appointments` |

> Detalhamento completo das tabelas e relacionamentos: ver `erd.md`.

---

## 5. Infraestrutura (Docker Compose)

```
┌────────────────────────────────────────────────────────────────────┐
│                         Docker Network                              │
│                                                                    │
│  postgres:15            :5432                                      │
│  rabbitmq:3-management  :5672 (AMQP) / :15672 (Management UI)     │
│  auth-service           :8082                                      │
│  queue-service          :8080                                      │
│  notification-service   :8081                                      │
│  regulacao-service      :8083                                      │
│  agendamento-service    :8084                                      │
└────────────────────────────────────────────────────────────────────┘
```

**Ordem de startup (depends_on + healthchecks):**
```
postgres (healthy)
    ↓
rabbitmq (healthy)
    ↓                ↓               ↓                ↓
auth-service   queue-service   regulacao-service   agendamento-service
                    ↓
           notification-service
```

**Healthchecks:** todos os serviços Spring Boot (`auth`, `queue`, `regulacao`, `agendamento`) expõem `/actuator/health` (permitido sem autenticação no `SecurityConfig` de cada um) via `spring-boot-starter-actuator`, e o `docker-compose.yml` usa esse endpoint — não `/v3/api-docs`, que fica desabilitado no profile `docker` (`springdoc.api-docs.enabled=false`) e portanto nunca respondeu. O `notification-service` (Quarkus) expõe `/q/health` via `quarkus-smallrye-health`.

> `regulacao-service` e `agendamento-service` já estão declarados no `docker-compose.yml`, cada um com seu `docker/Dockerfile` multi-stage (mesmo padrão dos demais serviços Spring Boot) — os cinco microsserviços sobem juntos via `docker-compose up`.

---

## 6. Segurança

### Fluxo JWT

```
1. Cliente faz POST /auth/login com { username, password }
2. auth-service valida credenciais no banco
3. auth-service gera JWT:
     header:  { alg: "HS256", typ: "JWT" }
     payload: { sub: "username", role: "REGULADOR", iat: ..., exp: ... }
     assinado com JWT_SECRET (variável de ambiente)
     — nota: o claim "role" NÃO tem o prefixo ROLE_ (JwtService faz
       claim("role", user.getRole().name())); o prefixo é adicionado
       por cada serviço consumidor ao montar a authority do Spring Security
4. Cliente envia token em todas as requests:
     Authorization: Bearer <token>
5. JwtAuthenticationFilter em queue/regulacao/agendamento valida a assinatura
   localmente, extrai a role, normaliza para ROLE_<valor> e popula o SecurityContext
6. Spring Security avalia a role contra as regras do SecurityConfig
```

### Matriz de permissões — serviços existentes

| Endpoint                            | MEDICO | PACIENTE |
|-------------------------------------|--------|----------|
| POST /api/v1/queue                  | ✅     | ❌       |
| GET  /api/v1/queue                  | ✅     | ❌       |
| GET  /api/v1/queue/{id}/position    | ✅     | ✅       |
| POST /api/v1/queue/call-next        | ✅     | ❌       |
| PATCH /api/v1/queue/{id}/priority   | ✅     | ❌       |
| DELETE /api/v1/queue/{id}           | ✅     | ❌       |
| POST /api/v1/patients               | ✅     | ❌       |
| GET  /api/v1/patients/**            | ✅     | ❌       |
| GET  /api/v1/procedures/**          | ✅     | ✅       |
| POST/GET /api/v1/quotas              | ✅     | ❌       |

### Matriz de permissões — novos serviços

| Endpoint                                      | SOLICITANTE | REGULADOR | MEDICO | PACIENTE | EXECUTANTE |
|-----------------------------------------------|-------------|-----------|--------|----------|------------|
| POST /api/v1/solicitacoes                     | ✅          | ❌        | ❌     | ❌       | ❌         |
| GET  /api/v1/solicitacoes                     | ✅          | ✅        | ❌     | ❌       | ❌         |
| DELETE /api/v1/solicitacoes/{id}              | ✅          | ❌        | ✅     | ❌       | ❌         |
| POST /api/v1/solicitacoes/{id}/complementar   | ✅          | ❌        | ❌     | ❌       | ❌         |
| POST /api/v1/regulacao/{id}/avaliar           | ❌          | ✅        | ❌     | ❌       | ❌         |
| GET  /api/v1/regulacao/pendentes              | ❌          | ✅        | ❌     | ❌       | ❌         |
| PATCH /api/v1/regulacao/solicitacoes/{id}/risco | ❌        | ✅        | ❌     | ❌       | ❌         |
| POST/GET /api/v1/regulacao/cotas              | ❌          | ✅        | ❌     | ❌       | ❌         |
| POST /api/v1/unidades-solicitantes            | ❌          | ✅        | ❌     | ❌       | ❌         |
| GET  /api/v1/unidades-solicitantes/{id}       | ✅          | ✅        | ❌     | ❌       | ❌         |
| GraphQL queries (agendamento)                 | ❌          | ✅        | ✅     | ✅*      | ✅         |
| PATCH /api/v1/appointments/{id}/confirmar     | ❌          | ❌        | ❌     | ✅       | ❌         |
| PATCH /api/v1/appointments/{id}/attend        | ❌          | ❌        | ❌     | ❌       | ✅         |
| DELETE /api/v1/appointments/{id}              | ❌          | ❌        | ✅     | ✅       | ❌         |
| PATCH /api/v1/appointments/{id}/reagendar     | ❌          | ❌        | ✅     | ❌       | ❌         |
| POST  /api/v1/appointments/{id}/falta         | ❌          | ❌        | ❌     | ❌       | ✅         |
| POST  /api/v1/schedules                       | ❌          | ❌        | ❌     | ❌       | ✅         |
| PUT   /api/v1/schedules/{id}                  | ❌          | ❌        | ❌     | ❌       | ✅         |
| POST  /api/v1/schedules/{id}/block            | ❌          | ❌        | ❌     | ❌       | ✅         |
| POST  /api/v1/schedules/{id}/unblock          | ❌          | ❌        | ❌     | ❌       | ✅         |

> * PACIENTE vê apenas seus próprios agendamentos (filtro por patientId extraído do JWT).

---

## 7. Variáveis de Ambiente

### auth-service / queue-service

| Variável                    | Descrição                          |
|-----------------------------|------------------------------------|
| `SPRING_DATASOURCE_URL`     | URL JDBC do PostgreSQL             |
| `SPRING_DATASOURCE_USERNAME`| Usuário do banco                   |
| `SPRING_DATASOURCE_PASSWORD`| Senha do banco                     |
| `SPRING_RABBITMQ_HOST`      | Host do RabbitMQ (queue-service)   |
| `SPRING_RABBITMQ_PORT`      | Porta AMQP (queue-service)         |
| `SPRING_RABBITMQ_USERNAME`  | Usuário RabbitMQ (queue-service)   |
| `SPRING_RABBITMQ_PASSWORD`  | Senha RabbitMQ (queue-service)     |
| `JWT_SECRET`                | Chave HMAC-SHA256 ≥ 256 bits       |
| `JWT_EXPIRATION_MS`         | Validade do token em ms (default: 86400000). Lido via `jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}` no auth-service. |

### notification-service (Quarkus)

| Variável                         | Descrição              |
|----------------------------------|------------------------|
| `QUARKUS_DATASOURCE_JDBC_URL`    | URL JDBC do PostgreSQL |
| `QUARKUS_DATASOURCE_USERNAME`    | Usuário do banco       |
| `QUARKUS_DATASOURCE_PASSWORD`    | Senha do banco         |
| `RABBITMQ_HOST/PORT/USERNAME/PASSWORD` | Conexão RabbitMQ |

### regulacao-service

| Variável                    | Descrição                    |
|-----------------------------|------------------------------|
| `SPRING_DATASOURCE_URL`     | URL JDBC do PostgreSQL       |
| `SPRING_DATASOURCE_USERNAME`| Usuário do banco             |
| `SPRING_DATASOURCE_PASSWORD`| Senha do banco               |
| `SPRING_RABBITMQ_*`         | Conexão RabbitMQ             |
| `JWT_SECRET`                | Mesmo valor dos demais       |

### agendamento-service

| Variável                    | Descrição                              |
|-----------------------------|----------------------------------------|
| `SPRING_DATASOURCE_URL`     | URL JDBC do PostgreSQL                 |
| `SPRING_DATASOURCE_USERNAME`| Usuário do banco                       |
| `SPRING_DATASOURCE_PASSWORD`| Senha do banco                         |
| `SPRING_RABBITMQ_*`         | Conexão RabbitMQ                       |
| `JWT_SECRET`                | Mesmo valor dos demais                 |
| `AGENDAMENTO_EXPIRACAO_HORAS`| Prazo para confirmação em horas (default: 72). Lido via `agendamento.expiracao-horas=${AGENDAMENTO_EXPIRACAO_HORAS:72}`; o job `@Scheduled` que verifica expiração roda a cada 15 min. |

> ⚠️ `JWT_SECRET` deve ser **idêntico** em todos os serviços que validam JWT (queue, regulacao, agendamento). Recomendado externalizar via `.env` no Docker Compose ou secrets manager em produção.

---

## 8. Decisões de Arquitetura

### Clean Architecture no queue-service
O queue-service é o núcleo de negócio. As regras de prioridade, os grupos legais e o algoritmo de ordenação são complexos e precisam ser testáveis de forma isolada. A Clean Architecture garante que o domínio nunca importa Spring, JPA ou RabbitMQ.

### Hexagonal Architecture no regulacao-service
O regulacao-service tem múltiplos adapters de entrada (REST hoje, potencialmente um portal web amanhã) e múltiplos adapters de saída (RabbitMQ, potencialmente SISREG real). Hexagonal torna esses adapters intercambiáveis sem tocar no domínio.

### CQRS no agendamento-service
O agendamento-service tem escrita simples (reservar slot, cancelar) mas leitura complexa (disponibilidade cruzando grade, capacidade, faixa de idade, preferência de unidade). CQRS separa esses dois lados, permitindo otimizar cada um de forma independente.

### GraphQL apenas nas queries do agendamento-service
Queries de disponibilidade têm muitas combinações de filtros e os clientes precisam de projeções diferentes (lista resumida vs detalhe completo). GraphQL elimina over-fetching e multiple round-trips. Commands (writes) continuam em REST por serem operações simples e bem definidas.

### MVC no auth-service
Responsabilidade única e bem delimitada (autenticar + emitir JWT). Sem lógica de domínio complexa — MVC é mais direto.

### Quarkus no notification-service
Startup rápido e menor footprint para um serviço que fica sempre em escuta. SmallRye Reactive Messaging integra nativamente com RabbitMQ de forma reativa.

### Por que 5 serviços e não um monólito?
Cada serviço pode escalar, ser implantado e evoluir de forma independente. O regulacao-service e o agendamento-service podem ser adicionados ao sistema já em produção sem downtime nos demais serviços.

### Cota opt-in no queue-service
A cota de `FILA_ESPERA` (`UnitProcedureQuota`) só se aplica quando alguém cadastra explicitamente um limite para uma combinação unidade+procedimento; sem cota configurada, o cadastro é livre. Isso evita que a ausência de dados de cota bloqueie o fluxo padrão de uma unidade que ainda não definiu limites.

---

## 9. Como Executar Localmente

```bash
# Pré-requisito: Docker e Docker Compose instalados

# Clonar o repositório
git clone <url-do-repo>
cd Morbus

# Criar o arquivo .env na raiz
echo "JWT_SECRET=sua-chave-secreta-muito-longa-aqui-minimo-256-bits" > .env

# Subir infraestrutura + todos os serviços
docker-compose up --build

# Verificar saúde
docker-compose ps
```

**URLs disponíveis após o startup:**

| Serviço               | Swagger / Docs                                   |
|-----------------------|--------------------------------------------------|
| notification-service  | http://localhost:8081/q/swagger-ui               |
| RabbitMQ Management   | http://localhost:15672 (admin / admin)           |

> ⚠️ **Swagger UI/GraphiQL não funcionam via `docker-compose up`** para `auth`, `queue`, `regulacao` e `agendamento`: o profile `docker` desabilita `springdoc.api-docs`/`swagger-ui` (postura de produção) e o `spring.graphql.graphiql.enabled` do agendamento-service só é `true` no profile `local`. Para explorar essas UIs, rode o serviço específico localmente com `SPRING_PROFILES_ACTIVE=local` (ou `dev`, no caso do notification-service) apontando para a infraestrutura do compose. O `notification-service` (Quarkus) é a única exceção — mantém `/q/swagger-ui` habilitado em produção.

**Fluxo de teste básico:**
1. `POST /auth/register` → criar usuários com roles MEDICO, SOLICITANTE, REGULADOR, EXECUTANTE, PACIENTE
2. `POST /auth/login` → obter JWT de cada perfil
3. `POST /api/v1/solicitacoes` (SOLICITANTE) → criar solicitação de procedimento
4. `POST /api/v1/regulacao/{id}/avaliar` (REGULADOR) → aprovar com riskColor
5. Verificar que entrada foi criada na fila (`GET /api/v1/queue`)
6. `POST /api/v1/queue/call-next` (MEDICO) → chamar próximo
7. Verificar disponibilidade via GraphQL: `query { disponibilidade(...) }`
8. `PATCH /api/v1/appointments/{id}/confirmar` (PACIENTE) → confirmar presença
9. `GET /api/v1/notifications` → verificar notificações geradas em cada etapa

---

*Documento atualizado em: julho/2026 — revisado para refletir o código real de todos os 5 serviços (nomes de pacote/classe, topologia RabbitMQ, GraphQL, cota, healthchecks e docker-compose).*
