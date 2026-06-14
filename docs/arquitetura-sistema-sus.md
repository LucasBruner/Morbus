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
└── src/main/java/br.com.sus.auth/
    ├── config/       # SecurityConfig, JwtConfig, OpenApiConfig
    ├── controller/   # AuthController
    ├── model/        # User.java, UserRole.java (enum)
    │   └── dto/      # RegisterRequest, LoginRequest, AuthResponse
    ├── repository/   # UserRepository
    └── service/      # AuthService, JwtService
```

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
    │   ├── enums/        # ERiskColor, EPriorityGroup, EQueueStatus, EGender, ETipoFila
    │   ├── event/        # IQueueEventPublisher (contrato de saída)
    │   ├── exceptions/   # QueueNotExistException, QueueNotAllowedException,
    │   │                 # QueueEmptyException, QuotaExceededException
    │   ├── repository/   # IQueueEntryRepository, IPatientRepository,
    │   │                 # IProcedureRepository, IUnitProcedureQuotaRepository
    │   ├── service/      # PriorityCalculator
    │   └── usecase/      # CallNextPatient, ReclassifyPriority, AddToQueue,
    │       └── DTO/      # CancelQueueEntry, CheckAndEnforceQuota
    ├── infrastructure/
    │   ├── config/       # RabbitMQConfig
    │   ├── messaging/    # RabbitMqQueueEventPublisher
    │   │   ├── consumer/ # SolicitacaoEventConsumer, AgendamentoEventConsumer
    │   │   └── dto/      # QueueEventDTO, SolicitacaoApprovedEventDTO, AppointmentEventDTO
    │   ├── persistence/  # entidades JPA e repositórios Spring Data
    │   └── security/     # JwtAuthFilter, SecurityConfig
    └── interfaces/
        ├── controller/   # QueueController, PatientController, ProcedureController
        └── dto/          # Request/Response DTOs, Mappers
```

**Regra de dependências:**
```
interfaces → application → domain ← infrastructure
```
Nenhuma classe do `domain` importa Spring, JPA ou RabbitMQ.

**Endpoints expostos:**

| Método | Path                        | Role       | Descrição                          |
|--------|-----------------------------|------------|------------------------------------|
| POST   | /api/v1/queue               | MEDICO     | Cadastra paciente na fila          |
| GET    | /api/v1/queue               | MEDICO     | Lista fila ordenada por prioridade |
| GET    | /api/v1/queue/{id}/position | PACIENTE   | Posição do paciente na fila        |
| POST   | /api/v1/queue/call-next     | MEDICO     | Chama o próximo da fila            |
| PATCH  | /api/v1/queue/{id}/priority | MEDICO     | Reclassifica cor de risco          |
| DELETE | /api/v1/queue/{id}          | MEDICO     | Cancela entrada na fila            |
| POST   | /api/v1/patients            | MEDICO     | Cadastra paciente                  |
| GET    | /api/v1/patients/{id}       | MEDICO     | Busca paciente por ID              |
| GET    | /api/v1/procedures          | PACIENTE   | Lista procedimentos disponíveis    |
| GET    | /api/v1/procedures/{id}     | PACIENTE   | Busca procedimento por ID          |

**Tipos de fila (ETipoFila):**

| Tipo            | Cor de risco     | Algoritmo de ordenação                  | Controle de cota |
|-----------------|------------------|-----------------------------------------|------------------|
| `FILA_ESPERA`   | Sempre `AZUL`    | Cronológico puro (registeredAt ASC)     | Sim — por UBS    |
| `FILA_REGULADA` | Definido pelo regulador | riskColor → priorityGroup → registeredAt | Não         |

> FILA_REGULADA sempre precede FILA_ESPERA na ordenação global.

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
└── src/main/java/br.com.sus.notification/
    ├── config/      # RabbitMQ bindings, OpenApiConfig
    ├── consumer/    # QueueEventConsumer, AgendamentoEventConsumer,
    │                # RegulacoesEventConsumer
    ├── controller/  # NotificationController
    ├── model/       # Notification.java (@Entity Panache)
    │   └── dto/     # QueueEventDTO, AppointmentEventDTO, SolicitacaoEventDTO
    ├── repository/  # NotificationRepository (PanacheRepository)
    └── service/     # NotificationService, EmailService
```

**Eventos consumidos:**

| Fila RabbitMQ                    | Evento                  | Destinatário       |
|----------------------------------|-------------------------|--------------------|
| queue.patient.registered         | PATIENT_REGISTERED      | Paciente           |
| queue.patient.called             | PATIENT_CALLED          | Paciente           |
| queue.priority.updated           | PRIORITY_UPDATED        | Paciente           |
| queue.patient.cancelled          | PATIENT_CANCELLED       | Paciente           |
| queue.patient.reinstated         | PATIENT_REINSTATED      | Paciente           |
| notifications.agendamento        | APPOINTMENT_CONFIRMED   | Paciente           |
| notifications.agendamento        | APPOINTMENT_CANCELLED   | Paciente           |
| notifications.agendamento        | APPOINTMENT_RESCHEDULED | Paciente           |
| notifications.agendamento        | APPOINTMENT_NO_SLOT     | Paciente           |
| notifications.agendamento        | APPOINTMENT_EXPIRED     | Paciente           |
| notifications.regulacao          | SOLICITATION_DENIED     | UBS Solicitante    |
| notifications.regulacao          | SOLICITATION_DEVOLVED   | UBS Solicitante    |

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
    │   ├── model/        # Solicitacao, Parecer, UnidadeSolicitante
    │   ├── enums/        # EStatusSolicitacao, EDecisaoRegulador, EDestino
    │   └── exception/    # SolicitacaoNotFoundException, RegulacaoNotAllowedException,
    │                     # DuplicateSolicitacaoException, QuotaExceededException
    ├── ports/
    │   ├── in/           # CriarSolicitacaoUseCase, AvaliarSolicitacaoUseCase,
    │   │                 # ComplementarSolicitacaoUseCase, ListarPendentesUseCase
    │   └── out/          # ISolicitacaoRepository, IParecerRepository,
    │                     # IQueueEventPort, INotificationPort
    └── adapters/
        ├── in/
        │   └── rest/     # SolicitacaoController, RegulacaoController, DTOs
        └── out/
            ├── jpa/      # SolicitacaoJpaAdapter, ParecerJpaAdapter,
            │             # entidades JPA, Spring Data repositories
            ├── rabbitmq/ # QueueEventRabbitAdapter, NotificationRabbitAdapter
            └── security/ # JwtAuthFilter, SecurityConfig
```

**Regra de dependências (Hexagonal):**
```
adapters/in ──▶ ports/in ──▶ domain ◀── ports/out ◀── adapters/out
```
O domínio não conhece Spring, JPA, RabbitMQ nem os adapters.

**Decisões do regulador (EDecisaoRegulador):**

| Decisão       | Efeito                                                              |
|---------------|---------------------------------------------------------------------|
| `AUTORIZAR`   | Entra em FILA_REGULADA com riskColor definido pelo regulador        |
| `FILA_ESPERA` | Entra em FILA_ESPERA com riskColor fixo AZUL                        |
| `NEGAR`       | Solicitação negada — notifica UBS com justificativa                 |
| `DEVOLVER`    | Dados incompletos — UBS deve complementar e reenviar               |
| `PENDENTE`    | Aprovado mas sem vaga disponível — aguarda abertura de cota         |

**Endpoints expostos:**

| Método | Path                                  | Role                    | Descrição                                   |
|--------|---------------------------------------|-------------------------|---------------------------------------------|
| POST   | /api/v1/solicitacoes                  | SOLICITANTE             | Cria nova solicitação                       |
| GET    | /api/v1/solicitacoes                  | SOLICITANTE / REGULADOR | Lista solicitações (filtros)                |
| GET    | /api/v1/solicitacoes/{id}             | SOLICITANTE / REGULADOR | Detalhe + histórico de pareceres            |
| POST   | /api/v1/solicitacoes/{id}/complementar| SOLICITANTE             | Complementa solicitação devolvida           |
| POST   | /api/v1/regulacao/{id}/avaliar        | REGULADOR               | Emite parecer (decisão + riskColor)         |
| GET    | /api/v1/regulacao/pendentes           | REGULADOR               | Lista solicitações aguardando avaliação     |
| GET    | /api/v1/regulacao/pendentes-vaga      | REGULADOR               | Lista aprovadas sem vaga disponível         |

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
| Docs           | SpringDoc OpenAPI + GraphiQL (`/graphiql`)          |
| API            | REST (commands) + GraphQL (queries)                 |
| Scheduler      | Spring `@Scheduled` — job de expiração 72h          |

**Estrutura de pacotes (CQRS):**
```
agendamento-service/
└── src/main/java/br.com.morbus.agendamento/
    ├── domain/
    │   ├── model/        # HealthUnit, Provider, Schedule, Slot, Appointment
    │   ├── enums/        # ESlotStatus, EAppointmentStatus
    │   └── exception/    # SlotNotFoundException, SlotUnavailableException,
    │                     # AppointmentNotFoundException, ExpiredConfirmationException
    ├── application/
    │   ├── command/      # AlocarSlotCommand, CancelarAgendamentoCommand,
    │   │   └── handler/  # ReagendarCommand, RegistrarFaltaCommand
    │   │                 # (handlers executam writes no banco transacional)
    │   └── query/        # DisponibilidadeQuery, AgendamentosQuery, GradeQuery
    │       └── handler/  # (handlers executam reads — podem usar projeções otimizadas)
    ├── infrastructure/
    │   ├── persistence/  # entidades JPA, Spring Data repositories
    │   ├── messaging/    # PatientCalledConsumer (@RabbitListener)
    │   │   └── publisher/# AgendamentoEventPublisher
    │   ├── scheduler/    # ExpiracaoAgendamentoJob (@Scheduled)
    │   ├── security/     # JwtAuthFilter, SecurityConfig
    │   └── config/       # RabbitMQConfig, GraphQLConfig
    └── interfaces/
        ├── rest/         # AppointmentController, SlotController, ScheduleController
        │   └── dto/      # Request/Response DTOs
        └── graphql/      # AgendamentoQueryResolver, DisponibilidadeQueryResolver
            └── type/     # AppointmentType, SlotType, ScheduleType (GraphQL schema types)
```

**Separação CQRS:**

| Lado    | Operação                                | Canal |
|---------|-----------------------------------------|-------|
| Command | AlocarSlot, Cancelar, Reagendar, Falta  | REST  |
| Command | VerificarExpiracoes (72h)               | `@Scheduled` |
| Query   | Disponibilidade de slots                | GraphQL |
| Query   | Agendamentos do paciente/unidade        | GraphQL |
| Query   | Grade semanal da unidade                | GraphQL |

**Schema GraphQL (consultas disponíveis):**
```graphql
type Query {
  disponibilidade(
    procedureId: ID!
    unitId: ID
    dateFrom: String!
    dateTo: String!
  ): [Slot!]!

  agendamentos(
    patientId: ID
    unitId: ID
    status: AppointmentStatus
    dateFrom: String
    dateTo: String
  ): [Appointment!]!

  grade(unitId: ID!, week: String!): [Schedule!]!
  agendamento(id: ID!): Appointment
}
```

**Endpoints REST (commands):**

| Método | Path                                 | Role       | Descrição                        |
|--------|--------------------------------------|------------|----------------------------------|
| PATCH  | /api/v1/appointments/{id}/confirmar  | PACIENTE   | Confirma presença (prazo 72h)    |
| DELETE | /api/v1/appointments/{id}            | PACIENTE / MEDICO | Cancela agendamento       |
| PATCH  | /api/v1/appointments/{id}/reagendar  | MEDICO     | Reagenda para outro slot         |
| POST   | /api/v1/appointments/{id}/falta      | EXECUTANTE | Registra falta do paciente       |
| POST   | /api/v1/schedules                    | EXECUTANTE | Cria grade semanal               |
| PUT    | /api/v1/schedules/{id}               | EXECUTANTE | Atualiza grade                   |
| POST   | /api/v1/schedules/{id}/bloquear      | EXECUTANTE | Bloqueia slots (feriado etc.)    |

---

## 3. Comunicação entre Serviços

### 3.1 Autenticação (síncrona — REST/JWT)

O `auth-service` emite o JWT. Todos os demais serviços validam o token localmente com o `JWT_SECRET` compartilhado, sem chamadas ao auth-service em runtime.

```
Cliente ──POST /auth/login──▶ auth-service
         ◀── { token: "eyJ..." } ──

Cliente ──Request (Bearer eyJ...)──▶ qualquer serviço
                                     JwtAuthFilter valida localmente
                                     extrai role → SecurityContext
```

### 3.2 Topologia RabbitMQ completa

```
sus.regulacao.exchange (direct)        publicado por: regulacao-service
├── solicitation.approved  →  queue.solicitation.approved
│      consumido por: queue-service, notification-service
├── solicitation.denied    →  queue.solicitation.denied
│      consumido por: notification-service
└── solicitation.devolved  →  queue.solicitation.devolved
       consumido por: notification-service

sus.queue.exchange (direct)            publicado por: queue-service
├── patient.registered     →  queue.patient.registered
│      consumido por: notification-service
├── patient.called         →  queue.patient.called
│      consumido por: agendamento-service, notification-service
├── patient.reinstated     →  queue.patient.reinstated
│      consumido por: notification-service
├── priority.updated       →  queue.priority.updated
│      consumido por: notification-service
└── patient.cancelled      →  queue.patient.cancelled
       consumido por: notification-service

sus.agendamento.exchange (direct)      publicado por: agendamento-service
├── appointment.confirmed   →  queue.appointment.confirmed
│      consumido por: queue-service, notification-service
├── appointment.cancelled   →  queue.appointment.cancelled
│      consumido por: queue-service, notification-service
├── appointment.rescheduled →  queue.appointment.rescheduled
│      consumido por: notification-service
├── appointment.no_slot     →  queue.appointment.no_slot
│      consumido por: queue-service, notification-service
├── appointment.expired     →  queue.appointment.expired
│      consumido por: queue-service, notification-service
└── patient.no_show         →  queue.patient.no_show
       consumido por: queue-service, notification-service
```

**Payload base dos eventos (JSON):**
```json
{
  "eventType": "PATIENT_CALLED",
  "queueEntryId": "uuid",
  "patientName": "João da Silva",
  "patientContact": "joao@email.com",
  "procedureName": "Consulta de Cardiologia",
  "procedureId": "uuid",
  "preferredUnitId": "uuid",
  "riskColor": "AMARELO",
  "tipoFila": "FILA_REGULADA",
  "timestamp": "2026-06-12T10:30:00Z"
}
```

---

## 4. Banco de Dados

Cada serviço possui seu próprio schema PostgreSQL. Não existem foreign keys entre schemas — a consistência entre serviços é garantida por eventos.

| Schema          | Serviço               | Tabelas principais                                             |
|-----------------|-----------------------|----------------------------------------------------------------|
| `auth`          | auth-service          | `users`                                                        |
| `queue`         | queue-service         | `patients`, `procedures`, `queue_entries`, `unit_procedure_quotas` |
| `notification`  | notification-service  | `notifications`                                                |
| `regulacao`     | regulacao-service     | `solicitacoes`, `pareceres`, `unidades_solicitantes`           |
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

---

## 6. Segurança

### Fluxo JWT

```
1. Cliente faz POST /auth/login com { username, password }
2. auth-service valida credenciais no banco
3. auth-service gera JWT:
     header:  { alg: "HS256", typ: "JWT" }
     payload: { sub: "username", role: "ROLE_REGULADOR", iat: ..., exp: ... }
     assinado com JWT_SECRET (variável de ambiente)
4. Cliente envia token em todas as requests:
     Authorization: Bearer <token>
5. JwtAuthFilter em cada serviço valida a assinatura localmente,
   extrai a role e popula o SecurityContext
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

### Matriz de permissões — novos serviços

| Endpoint                                      | SOLICITANTE | REGULADOR | MEDICO | PACIENTE | EXECUTANTE |
|-----------------------------------------------|-------------|-----------|--------|----------|------------|
| POST /api/v1/solicitacoes                     | ✅          | ❌        | ❌     | ❌       | ❌         |
| GET  /api/v1/solicitacoes                     | ✅          | ✅        | ❌     | ❌       | ❌         |
| POST /api/v1/solicitacoes/{id}/complementar   | ✅          | ❌        | ❌     | ❌       | ❌         |
| POST /api/v1/regulacao/{id}/avaliar           | ❌          | ✅        | ❌     | ❌       | ❌         |
| GET  /api/v1/regulacao/pendentes              | ❌          | ✅        | ❌     | ❌       | ❌         |
| GraphQL queries (agendamento)                 | ❌          | ✅        | ✅     | ✅*      | ✅         |
| PATCH /api/v1/appointments/{id}/confirmar     | ❌          | ❌        | ❌     | ✅       | ❌         |
| DELETE /api/v1/appointments/{id}              | ❌          | ❌        | ✅     | ✅       | ❌         |
| POST  /api/v1/appointments/{id}/falta         | ❌          | ❌        | ❌     | ❌       | ✅         |
| POST  /api/v1/schedules                       | ❌          | ❌        | ❌     | ❌       | ✅         |

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
| `JWT_EXPIRATION_MS`         | Validade do token (default: 86400000)|

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
| `AGENDAMENTO_EXPIRACAO_HORAS`| Prazo para confirmação (default: 72)  |

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
| queue-service         | http://localhost:8080/swagger-ui.html            |
| auth-service          | http://localhost:8082/swagger-ui.html            |
| notification-service  | http://localhost:8081/q/swagger-ui               |
| regulacao-service     | http://localhost:8083/swagger-ui.html            |
| agendamento-service   | http://localhost:8084/swagger-ui.html            |
| agendamento GraphiQL  | http://localhost:8084/graphiql                   |
| RabbitMQ Management   | http://localhost:15672 (admin / admin)           |

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

*Documento atualizado em: junho/2026*
