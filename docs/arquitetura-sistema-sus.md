# Arquitetura do Sistema — Fila SUS
> Hackathon FIAP PosTech — Arquitetura e Desenvolvimento Java — Fase 5

---

## 1. Visão Geral

O sistema digitaliza o processo de **fila regulatória ambulatorial do SUS (SISREG)**, cobrindo desde o cadastro de um paciente em um procedimento até a chamada do próximo da fila com base em regras de prioridade definidas pelo Ministério da Saúde.

O sistema é composto por **três microsserviços** independentes que se comunicam via REST (operações síncronas) e RabbitMQ (notificações assíncronas).

```
┌─────────────┐     REST/JWT      ┌──────────────────┐
│   Cliente   │ ───────────────▶  │   auth-service   │
│  (Postman / │                   │   :8082          │
│   Swagger)  │                   └──────────────────┘
│             │     REST/JWT      ┌──────────────────┐      AMQP       ┌───────────────────────┐
│             │ ───────────────▶  │  queue-service   │ ─────────────▶  │  notification-service │
└─────────────┘                   │  :8080           │                 │  :8081                │
                                  └────────┬─────────┘                 └───────────────────────┘
                                           │ JPA
                                  ┌────────▼─────────┐
                                  │   PostgreSQL :5432│
                                  └──────────────────┘
```

---

## 2. Serviços

### 2.1 auth-service

**Responsabilidade:** Autenticar usuários e emitir tokens JWT com a role do perfil.

| Atributo         | Valor                                      |
|------------------|--------------------------------------------|
| Framework        | Spring Boot 4 + Spring Security            |
| Padrão           | MVC                                        |
| Porta            | `8082`                                     |
| Banco de dados   | PostgreSQL (schema compartilhado)          |
| Autenticação     | JWT assinado com HMAC-SHA256               |

**Estrutura de pacotes:**
```
auth-service/
└── src/main/java/
    └── br.com.sus.auth/
        ├── config/          # SecurityConfig, JwtConfig, OpenApiConfig
        ├── controller/      # AuthController
        ├── model/           # User.java, UserRole.java (enum)
        │   └── dto/         # RegisterRequest, LoginRequest, AuthResponse
        ├── repository/      # UserRepository
        └── service/         # AuthService, JwtService
```

**Endpoints expostos:**

| Método | Path              | Auth | Descrição                          |
|--------|-------------------|------|------------------------------------|
| POST   | /auth/register    | ❌   | Cria novo usuário com role         |
| POST   | /auth/login       | ❌   | Valida credenciais e retorna JWT   |

**Roles disponíveis:**

| Role           | Descrição                                              |
|----------------|--------------------------------------------------------|
| `ROLE_MEDICO`  | Profissional de saúde — gerencia a fila completa       |
| `ROLE_PACIENTE`| Paciente — consulta apenas sua própria posição         |

---

### 2.2 queue-service

**Responsabilidade:** Núcleo do sistema — gerencia o cadastro de pacientes na fila, o algoritmo de prioridade, a chamada do próximo e a publicação de eventos.

| Atributo         | Valor                                                  |
|------------------|--------------------------------------------------------|
| Framework        | Spring Boot 4.0.6 + Spring Data JPA + Spring AMQP     |
| Padrão           | **Clean Architecture**                                 |
| Porta            | `8080`                                                 |
| Banco de dados   | PostgreSQL                                             |
| Migrations       | Flyway                                                 |
| Docs             | SpringDoc OpenAPI (Swagger UI em `/swagger-ui.html`)   |

**Estrutura de pacotes (Clean Architecture):**
```
queue-service/
└── src/main/java/
    └── br.com.morbus.queueservice/
        ├── domain/
        │   ├── entity/          # Patient, QueueEntry, Procedure (POJO puro, Lombok @Builder @Getter)
        │   ├── enums/           # ERiskColor, EPriorityGroup, EQueueStatus, EGender
        │   ├── event/           # IQueueEventPublisher (contrato de saída — porta do domínio)
        │   ├── exceptions/      # QueueNotExistException, QueueNotAllowedException, QueueEmptyException
        │   ├── repository/      # IQueueEntryRepository, IPatientRepository, IProcedureRepository
        │   ├── service/         # PriorityCalculator
        │   └── usecase/         # CallNextPatient, CancelQueueEntry, GetQueuePosition, ReclassifyPriority
        │       └── DTO/         # QueueCancelDTO, QueueUpdateRiskColorDTO, QueueEntryRiskQueuePosition
        ├── application/
        │   └── usecase/         # (reservado — use cases vivem em domain/usecase nesta fase)
        ├── infrastructure/
        │   ├── config/          # RabbitMQConfig
        │   ├── messaging/       # RabbitMqQueueEventPublisher
        │   │   └── DTO/         # QueueEventPayload (record)
        │   ├── persistence/     # (a implementar — entidades JPA e repositórios Spring Data)
        │   └── security/        # (a implementar — JwtAuthFilter, SecurityConfig)
        └── interfaces/
            ├── controller/      # (a implementar — QueueController, PatientController, ProcedureController)
            └── dto/             # (a implementar — Request/Response DTOs, Mappers)
```

**Regra de dependências (Clean Architecture):**
```
interfaces → application → domain ← infrastructure
```
Nenhuma classe do `domain` importa algo de `infrastructure`, `interfaces` ou qualquer framework (Spring, JPA, RabbitMQ).

**Endpoints expostos:**

| Método | Path                           | Role mínima    | Descrição                          |
|--------|--------------------------------|----------------|------------------------------------|
| POST   | /api/v1/queue                  | MEDICO         | Cadastra paciente na fila          |
| GET    | /api/v1/queue                  | MEDICO         | Lista fila ordenada por prioridade |
| GET    | /api/v1/queue/{id}/position    | PACIENTE       | Posição do paciente na fila        |
| POST   | /api/v1/queue/call-next        | MEDICO         | Chama o próximo da fila            |
| PATCH  | /api/v1/queue/{id}/priority    | MEDICO         | Reclassifica cor de risco          |
| DELETE | /api/v1/queue/{id}             | MEDICO         | Cancela entrada na fila            |
| POST   | /api/v1/patients               | MEDICO         | Cadastra paciente                  |
| GET    | /api/v1/patients/{id}          | MEDICO         | Busca paciente por ID              |
| GET    | /api/v1/procedures             | PACIENTE       | Lista procedimentos disponíveis    |
| GET    | /api/v1/procedures/{id}        | PACIENTE       | Busca procedimento por ID          |

**Algoritmo de prioridade:**
```sql
ORDER BY
  risk_color ASC,        -- VERMELHO(1) > AMARELO(2) > VERDE(3) > AZUL(4)
  priority_group ASC,    -- IDOSO(1) > GESTANTE(2) > DEFICIENTE(3) > LACTANTE(4) > OBESO(5) > GERAL(6)
  registered_at ASC      -- Quem chegou primeiro dentro do mesmo nível
```

**Classificação de risco (SISREG ambulatorial):**

| Cor        | Prioridade | Tempo máximo de espera |
|------------|------------|------------------------|
| VERMELHO   | 1          | 1 mês                  |
| AMARELO    | 2          | 3 meses                |
| VERDE      | 3          | 6 meses                |
| AZUL       | 4          | 1 ano (entrada padrão) |

**Grupos legais (Lei 10.048/2000 + Estatuto do Idoso):**

| Grupo      | Prioridade | Critério                   |
|------------|------------|----------------------------|
| IDOSO      | 1          | Idade ≥ 60 anos            |
| GESTANTE   | 2          | Campo gestante = true      |
| DEFICIENTE | 3          | Campo deficiente = true    |
| LACTANTE   | 4          | Campo lactante = true      |
| OBESO      | 5          | Campo obeso = true         |
| GERAL      | 6          | Default                    |

---

### 2.3 notification-service

**Responsabilidade:** Consumir eventos do RabbitMQ e enviar notificações (e-mail simulado em console) ao paciente afetado.

| Atributo         | Valor                                                         |
|------------------|---------------------------------------------------------------|
| Framework        | Quarkus + SmallRye Reactive Messaging + Hibernate Panache     |
| Padrão           | **MVC**                                                       |
| Porta            | `8081`                                                        |
| Banco de dados   | PostgreSQL                                                    |
| Docs             | Quarkus OpenAPI (Swagger UI em `/q/swagger-ui`)               |

**Estrutura de pacotes (MVC):**
```
notification-service/
└── src/main/java/
    └── br.com.sus.notification/
        ├── config/          # RabbitMQ bindings, OpenApiConfig
        ├── consumer/        # QueueEventConsumer (@Incoming)
        ├── controller/      # NotificationController
        ├── model/           # Notification.java (@Entity Panache)
        │   └── dto/         # QueueEventDTO (record)
        ├── repository/      # NotificationRepository (PanacheRepository)
        └── service/         # NotificationService, EmailService
```

**Eventos consumidos:**

| Fila RabbitMQ              | Tipo de evento      | Mensagem enviada ao paciente                                           |
|----------------------------|---------------------|------------------------------------------------------------------------|
| queue.patient.registered   | PATIENT_REGISTERED  | "Você foi cadastrado na fila para [procedimento]. Classificação: [cor]"|
| queue.patient.called       | PATIENT_CALLED      | "É a sua vez! Compareça ao guichê para [procedimento]."               |
| queue.priority.updated     | PRIORITY_UPDATED    | "Sua prioridade na fila foi atualizada para [cor]."                   |
| queue.patient.cancelled    | PATIENT_CANCELLED   | "Seu agendamento para [procedimento] foi cancelado."                  |

**Endpoints expostos:**

| Método | Path                       | Descrição                              |
|--------|----------------------------|----------------------------------------|
| GET    | /api/v1/notifications      | Lista notificações (paginado)          |
| GET    | /api/v1/notifications/{id} | Busca notificação por ID               |

---

## 3. Comunicação entre Serviços

### 3.1 Autenticação (síncrona — REST)

```
Cliente ──POST /auth/login──▶ auth-service
         ◀── { token: "eyJ..." } ──

Cliente ──GET /api/v1/queue (Bearer eyJ...)──▶ queue-service
                                               JwtAuthFilter valida token
                                               extrai role → SecurityContext
         ◀── 200 OK ou 401/403 ──
```

O `auth-service` e o `queue-service` **compartilham o mesmo `JWT_SECRET`**. O queue-service valida o token localmente, sem chamadas ao auth-service em runtime.

### 3.2 Eventos de fila (assíncrona — RabbitMQ)

```
queue-service
  │
  ├── publica em: sus.queue.exchange (direct)
  │     ├── routing key: patient.registered  →  fila: queue.patient.registered
  │     ├── routing key: patient.called      →  fila: queue.patient.called
  │     ├── routing key: priority.updated    →  fila: queue.priority.updated
  │     └── routing key: patient.cancelled   →  fila: queue.patient.cancelled
  │
notification-service
  │
  └── consome todas as 4 filas via @Incoming (SmallRye Reactive Messaging)
```

**Payload dos eventos (JSON) — `QueueEventPayload`:**
```json
{
  "eventType": "PATIENT_CALLED",
  "queueEntryId": "uuid",
  "patientName": "João da Silva",
  "patientContact": "joao@email.com",
  "procedureName": "Consulta de Cardiologia",
  "riskColor": "AMARELO",
  "motivoCancelamento": null,
  "timestamp": "2026-05-27T10:30:00Z"
}
```

> `motivoCancelamento` é preenchido apenas no evento `PATIENT_CANCELLED` (quando um motivo é fornecido); nos demais eventos o campo é `null`.

**IQueueEventPublisher — métodos do contrato:**

| Método                                               | Evento publicado   | Routing key         |
|------------------------------------------------------|--------------------|---------------------|
| `publishPatientRegistered(QueueEntry)`               | PATIENT_REGISTERED | `patient.registered`|
| `publishPatientCalled(QueueEntry)`                   | PATIENT_CALLED     | `patient.called`    |
| `publishPriorityUpdated(QueueEntry)`                 | PRIORITY_UPDATED   | `priority.updated`  |
| `publishPatientCancelled(QueueEntry, String reason)` | PATIENT_CANCELLED  | `patient.cancelled` |

> **Status do RabbitMQConfig:** atualmente apenas a fila `queue.patient.registered` está com binding declarado. As filas `queue.patient.called`, `queue.priority.updated` e `queue.patient.cancelled` têm Queue beans declarados mas ainda precisam de Binding.

---

## 4. Banco de Dados

Todos os serviços apontam para o **mesmo servidor PostgreSQL**, mas utilizam schemas/tabelas independentes (sem foreign keys entre serviços).

### Tabelas do auth-service

```sql
users
  id            UUID PRIMARY KEY
  username      VARCHAR(100) UNIQUE NOT NULL
  email         VARCHAR(255) UNIQUE NOT NULL
  password_hash VARCHAR(255) NOT NULL
  role          VARCHAR(20) NOT NULL  -- 'MEDICO' | 'PACIENTE'
  created_at    TIMESTAMP NOT NULL
```

### Tabelas do queue-service

```sql
procedures
  id               UUID PRIMARY KEY
  co_procedimento  VARCHAR(20) UNIQUE NOT NULL
  no_procedimento  VARCHAR(255) NOT NULL
  idade_minima     INTEGER
  idade_maxima     INTEGER
  grupo            VARCHAR(100)

patients
  id               UUID PRIMARY KEY
  cpf              VARCHAR(14) UNIQUE NOT NULL
  cns              VARCHAR(15) UNIQUE
  nome_completo    VARCHAR(255) NOT NULL
  data_nascimento  DATE NOT NULL
  sexo             CHAR(1)
  contato          VARCHAR(255)         -- e-mail ou telefone
  grupo_legal      VARCHAR(20) NOT NULL -- enum PriorityGroup

queue_entries
  id            UUID PRIMARY KEY
  patient_id    UUID NOT NULL REFERENCES patients(id)
  procedure_id  UUID NOT NULL REFERENCES procedures(id)
  risk_color    VARCHAR(10) NOT NULL  -- VERMELHO | AMARELO | VERDE | AZUL
  status        VARCHAR(20) NOT NULL  -- AGUARDANDO | AGENDADO | ATENDIDO | FALTOU | CANCELADO | DEVOLVIDO
  registered_at TIMESTAMP NOT NULL
  updated_at    TIMESTAMP
```

### Tabelas do notification-service

```sql
notifications
  id                UUID PRIMARY KEY
  event_type        VARCHAR(30) NOT NULL
  recipient_name    VARCHAR(255)
  recipient_contact VARCHAR(255)
  message           TEXT NOT NULL
  sent_at           TIMESTAMP NOT NULL
  status            VARCHAR(20) NOT NULL  -- SENT | FAILED
```

---

## 5. Infraestrutura (Docker Compose)

```
┌─────────────────────────────────────────────────────┐
│                  Docker Network                      │
│                                                     │
│  postgres:15          :5432                         │
│  rabbitmq:3-mgmt      :5672 (AMQP) / :15672 (UI)   │
│  auth-service         :8082                         │
│  queue-service        :8080                         │
│  notification-service :8081                         │
└─────────────────────────────────────────────────────┘
```

**Ordem de startup (depends_on + healthchecks):**
```
postgres (healthy)
    ↓
rabbitmq (healthy)
    ↓              ↓
auth-service   queue-service
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
     payload: { sub: "username", role: "ROLE_MEDICO", iat: ..., exp: ... }
     assinado com JWT_SECRET (variável de ambiente)
4. Cliente envia token em todas as requests:
     Authorization: Bearer <token>
5. JwtAuthFilter no queue-service intercepta a request,
   valida a assinatura com JWT_SECRET,
   extrai a role e popula o SecurityContext
6. Spring Security avalia a role contra as regras do SecurityConfig
```

### Matriz de permissões

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
| POST /auth/register                 | público| público  |
| POST /auth/login                    | público| público  |

---

## 7. Variáveis de Ambiente

### auth-service

| Variável              | Valor padrão (dev)        | Descrição                        |
|-----------------------|---------------------------|----------------------------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/sus_queue_db` | URL do banco |
| `SPRING_DATASOURCE_USERNAME` | `sus_user`           | Usuário do banco                 |
| `SPRING_DATASOURCE_PASSWORD` | `sus_pass`           | Senha do banco                   |
| `JWT_SECRET`          | *(obrigatório)*           | Segredo HMAC-SHA256 (≥ 256 bits) |
| `JWT_EXPIRATION_MS`   | `86400000` (24h)          | Validade do token em ms          |

### queue-service

| Variável              | Valor padrão (dev)        | Descrição                        |
|-----------------------|---------------------------|----------------------------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/sus_queue_db` | URL do banco |
| `SPRING_DATASOURCE_USERNAME` | `sus_user`           | Usuário do banco                 |
| `SPRING_DATASOURCE_PASSWORD` | `sus_pass`           | Senha do banco                   |
| `SPRING_RABBITMQ_HOST` | `localhost`              | Host do RabbitMQ                 |
| `SPRING_RABBITMQ_PORT` | `5672`                   | Porta AMQP                       |
| `SPRING_RABBITMQ_USERNAME` | `admin`              | Usuário do RabbitMQ              |
| `SPRING_RABBITMQ_PASSWORD` | `admin`              | Senha do RabbitMQ                |
| `JWT_SECRET`          | *(obrigatório)*           | Mesmo valor do auth-service      |

### notification-service

| Variável                             | Valor padrão (dev) | Descrição               |
|--------------------------------------|--------------------|-------------------------|
| `QUARKUS_DATASOURCE_JDBC_URL`        | `jdbc:postgresql://localhost:5432/sus_queue_db` | URL do banco |
| `QUARKUS_DATASOURCE_USERNAME`        | `sus_user`         | Usuário do banco        |
| `QUARKUS_DATASOURCE_PASSWORD`        | `sus_pass`         | Senha do banco          |
| `RABBITMQ_HOST`                      | `localhost`        | Host do RabbitMQ        |
| `RABBITMQ_PORT`                      | `5672`             | Porta AMQP              |
| `RABBITMQ_USERNAME`                  | `admin`            | Usuário do RabbitMQ     |
| `RABBITMQ_PASSWORD`                  | `admin`            | Senha do RabbitMQ       |

> ⚠️ `JWT_SECRET` deve ser idêntico nos dois serviços que o utilizam (`auth-service` e `queue-service`). Recomendado externalizar via `.env` no Docker Compose ou secrets manager em produção.

---

## 8. Decisões de Arquitetura

### Por que Clean Architecture no queue-service?

O queue-service é o núcleo do negócio. As regras de prioridade, os grupos legais e o algoritmo de ordenação são complexos e precisam ser testáveis de forma isolada, sem depender de banco de dados ou framework. A Clean Architecture garante que a lógica de domínio (`PriorityCalculator`, `QueueEntry`, enums) nunca importa nada de Spring, JPA ou RabbitMQ — o que facilita testes unitários puros.

### Por que MVC no notification-service?

O notification-service tem uma responsabilidade simples e bem definida: ouvir eventos e disparar notificações. Não há lógica de domínio complexa, não há casos de uso com múltiplas variantes. MVC é mais direto e menos burocrático para esse nível de complexidade.

### Por que Quarkus no notification-service?

Quarkus tem startup mais rápido e menor footprint de memória, ideal para um serviço que fica sempre em escuta (long-running listener). O SmallRye Reactive Messaging integra nativamente com RabbitMQ/AMQP de forma reativa.

### Por que auth-service separado e não Spring Security no próprio queue-service?

Um auth-service dedicado permite que futuros serviços (ex: um serviço de agendamento, um portal do paciente) reutilizem a autenticação sem duplicar código. Também demonstra uma arquitetura mais realista de microsserviços, onde a responsabilidade de autenticação é isolada.

### Por que JWT stateless e não sessão?

Microsserviços não compartilham estado de sessão. JWT permite que o queue-service valide a identidade do usuário localmente (sem consultar o auth-service em cada request), mantendo a independência entre os serviços.

---

## 9. Como Executar Localmente

```bash
# Pré-requisito: Docker e Docker Compose instalados

# Clonar o repositório
git clone <url-do-repo>
cd <nome-do-repo>

# Criar o arquivo .env na raiz (ou exportar as variáveis)
echo "JWT_SECRET=sua-chave-secreta-muito-longa-aqui-minimo-256-bits" > .env

# Subir todos os serviços
docker-compose up --build

# Verificar se está tudo saudável
docker-compose ps
```

**URLs disponíveis após o startup:**

| Serviço               | URL                                          |
|-----------------------|----------------------------------------------|
| queue-service API     | http://localhost:8080/swagger-ui.html        |
| auth-service API      | http://localhost:8082/swagger-ui.html        |
| notification-service  | http://localhost:8081/q/swagger-ui           |
| RabbitMQ Management   | http://localhost:15672 (admin / admin)       |

**Fluxo de teste básico via Swagger:**
1. `POST /auth/register` no auth-service → criar um usuário MEDICO
2. `POST /auth/login` → obter o token JWT
3. Clicar em **Authorize** no Swagger do queue-service e colar `Bearer <token>`
4. `POST /api/v1/patients` → cadastrar um paciente
5. `POST /api/v1/queue` → inserir o paciente na fila
6. `POST /api/v1/queue/call-next` → chamar o próximo
7. Verificar `GET /api/v1/notifications` no notification-service → notificação gerada

---

*Documento gerado em: maio/2026*
