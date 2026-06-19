# Morbus

Sistema de gerenciamento de filas ambulatoriais do SUS — FIAP PosTech Fase 5 Hackathon.

---

## Sobre o Projeto

O SUS atende cerca de 150 milhões de brasileiros em procedimentos ambulatoriais especializados. A regulação dessas consultas é feita hoje pelo **SISREG**, mas o processo sofre com filas sem critério clínico objetivo, dificuldade de acompanhamento pelo paciente e ausência de notificação automática ao ser chamado.

**Morbus** resolve esse problema entregando uma fila digital inteligente com:

- **Priorização automática** baseada nas regras do SUS (cor de risco clínico, grupos de prioridade legal, tempo de espera).
- **Posição em tempo real** consultável pelo próprio paciente via API segura.
- **Notificações automáticas** quando o paciente é chamado, via RabbitMQ e e-mail.
- **Controle de acesso por papel** (médico vs. paciente), com autenticação JWT centralizada.

O sistema é composto por cinco microsserviços independentes e se comunica internamente por REST (autenticação), AMQP (eventos assíncronos via RabbitMQ) e GraphQL (consultas do agendamento-service).

---

## Arquitetura

```
                    ┌─────────────────────┐
                    │     auth-service     │
                    │    :8082  (JWT)      │
                    └──────────┬──────────┘
                               │ Bearer Token
                               ▼
┌──────────────────────────────────────────────────┐
│                  queue-service                    │
│                    :8080                          │
│  ┌────────────┐  ┌──────────────┐               │
│  │   Domain   │  │ PostgreSQL   │               │
│  │ (Clean Arch│  │   :5432      │               │
│  │  + SISREG) │  └──────────────┘               │
│  └─────┬──────┘                                  │
└────────┼─────────────────────────────────────────┘
         │ AMQP (RabbitMQ :5672)
         ▼
┌─────────────────────────┐
│   notification-service   │
│       :8081              │
│ (Quarkus + e-mail)       │
└──────────────────────────┘
```

| Serviço                | Porta | Stack                                  | Responsabilidade                                            |
|------------------------|-------|----------------------------------------|-------------------------------------------------------------|
| `auth-service`         | 8082  | Spring Boot 4, MVC, JWT                | Cadastro de usuários, login, emissão de tokens JWT          |
| `queue-service`        | 8080  | Spring Boot 4, Clean Arch, JPA, AMQP  | Núcleo do sistema: filas, pacientes, procedimentos          |
| `notification-service` | 8081  | Quarkus, Panache, Mailer               | Consome eventos do RabbitMQ e envia e-mails simulados       |
| `regulacao-service`    | 8083  | Spring Boot 4, Hexagonal Arch, JPA, AMQP | Avaliação regulatória: aprovação/negação de solicitações (*em desenvolvimento*) |
| `agendamento-service`  | 8084  | Spring Boot 4, CQRS, GraphQL, JPA, AMQP | Gestão de grades, slots e agendamentos (*em desenvolvimento*) |

**Comunicação entre serviços:**

- `auth-service` → todos: JWT — cada serviço valida o token localmente com a mesma `JWT_SECRET`.
- `queue-service` ↔ `notification-service`: AMQP via RabbitMQ — exchange `sus.queue.exchange`.
- `regulacao-service` → `queue-service` / `agendamento-service`: AMQP — exchange `sus.regula.exchange`.
- `agendamento-service` → `queue-service` / `notification-service`: AMQP — exchange `sus.agenda.exchange`.

---

## Tecnologias

| Categoria      | Tecnologia                                      |
|----------------|-------------------------------------------------|
| Linguagem      | Java 21                                         |
| Frameworks     | Spring Boot 4.0.6 (queue-service, auth-service) · Quarkus (notification-service) |
| Persistência   | Spring Data JPA · Hibernate · Flyway · PostgreSQL 15 |
| Mensageria     | RabbitMQ 3 (Spring AMQP)                        |
| Segurança      | Spring Security · JWT (HMAC-SHA256)             |
| Documentação   | SpringDoc OpenAPI 3 (Swagger UI)                |
| Infraestrutura | Docker · Docker Compose                         |
| Testes         | JUnit 5 · Mockito · H2 (in-memory)              |
| Build          | Maven (Wrapper incluído)                        |

---

## Pré-requisitos

- **Docker** e **Docker Compose** (obrigatório para subir todos os serviços)
- **Java 21** e **Maven 3.9+** (apenas para desenvolvimento local / execução individual)

---

## Como Executar

### 1. Configurar variáveis de ambiente

Crie um arquivo `.env` na raiz do repositório:

```env
# Segredo usado para assinar e validar tokens JWT.
# OBRIGATÓRIO. Deve ser uma string aleatória de pelo menos 32 caracteres.
# Deve ser IDÊNTICO no auth-service e no queue-service.
JWT_SECRET=sua-chave-secreta-longa-e-aleatoria-aqui
```

> ⚠️ **Nunca commite o `.env` com valores reais.** O arquivo `.env` está no `.gitignore`.
> Use `openssl rand -base64 32` para gerar uma chave segura.

### 2. Subir todos os serviços

```bash
docker compose up --build
```

Isso sobe PostgreSQL, RabbitMQ e os três microsserviços em ordem. Na primeira execução, o Flyway cria automaticamente todas as tabelas.

### 3. Verificar se está tudo no ar

| Recurso                   | URL                                       |
|---------------------------|-------------------------------------------|
| Queue Service — Swagger   | http://localhost:8080/swagger-ui.html     |
| Auth Service — Swagger    | http://localhost:8082/swagger-ui.html     |
| RabbitMQ Management       | http://localhost:15672 (admin / admin)    |

### 4. Desenvolvimento local (serviço individual)

```bash
# Subir apenas a infraestrutura
docker compose up postgres rabbitmq

# Rodar o queue-service localmente
cd queue-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Rodar os testes
./mvnw test
```

---

## Autenticação

Todos os endpoints do `queue-service` exigem um **Bearer token JWT** emitido pelo `auth-service`.

### Fluxo de autenticação

```bash
# 1. Registrar usuário
curl -X POST http://localhost:8082/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "medico@sus.gov.br", "password": "senha123", "role": "MEDICO"}'

# 2. Fazer login e obter token
curl -X POST http://localhost:8082/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "medico@sus.gov.br", "password": "senha123"}'
# → resposta: { "token": "eyJ..." }

# 3. Usar o token nas requisições ao queue-service
curl -H "Authorization: Bearer eyJ..." http://localhost:8080/api/v1/queue
```

No Swagger UI do queue-service, clique em **Authorize** e informe `Bearer <token>`.

### Roles

| Role       | Permissões                                                                                             |
|------------|--------------------------------------------------------------------------------------------------------|
| `MEDICO`   | Cadastrar e consultar pacientes, gerenciar fila (cadastrar, listar, chamar próximo, reclassificar, cancelar), consultar procedimentos |
| `PACIENTE` | Consultar posição na fila, listar e buscar procedimentos                                               |

---

## Endpoints Principais

### Queue Service (`:8080`)

#### Fila

```bash
# Listar fila ordenada por prioridade
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/v1/queue

# Cadastrar paciente na fila
curl -X POST http://localhost:8080/api/v1/queue \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "uuid-do-paciente",
    "procedureId": "uuid-do-procedimento",
    "tipoFila": "FILA_REGULADA",
    "riskColor": "AMARELO"
  }'

# Chamar o próximo paciente
curl -X POST http://localhost:8080/api/v1/queue/call-next \
  -H "Authorization: Bearer <token>"

# Consultar posição na fila (PACIENTE)
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/v1/queue/{id}/position

# Reclassificar cor de risco
curl -X PATCH http://localhost:8080/api/v1/queue/{id}/priority \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"riskColor": "VERMELHO"}'

# Cancelar entrada na fila
curl -X DELETE http://localhost:8080/api/v1/queue/{id} \
  -H "Authorization: Bearer <token>"
```

#### Pacientes

```bash
# Cadastrar paciente
curl -X POST http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "cpf": "12345678900",
    "nome": "Maria",
    "sobrenome": "Silva",
    "dataNascimento": "1960-05-10",
    "gender": "FEMININO",
    "grupoLegal": "IDOSO"
  }'

# Buscar paciente por ID
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/v1/patients/{id}

# Buscar paciente por CPF
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/v1/patients?cpf=12345678900"

# Atribuir procedimento ao paciente
curl -X POST http://localhost:8080/api/v1/patients/{patientId}/procedures/{procedureId} \
  -H "Authorization: Bearer <token>"
```

#### Procedimentos

```bash
# Listar procedimentos (paginado)
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/v1/procedures?page=1&size=20"

# Buscar procedimento por ID
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/v1/procedures/{id}

# Buscar por código SIGTAP
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/v1/procedures?codigo=0301010129"
```

> Documentação interativa completa: **http://localhost:8080/swagger-ui.html**

---

## Algoritmo de Prioridade

A fila é ordenada por **4 critérios em cascata**, implementados em `PriorityCalculator` (domínio puro, sem dependências de framework):

### Critério 1 — Tipo de fila

`FILA_REGULADA` sempre precede `FILA_ESPERA`, independentemente de cor de risco ou tempo de espera. Casos regulados são urgentes ou prioritários; a fila de espera é para rotina.

### Critério 2 — Cor de risco clínico (somente `FILA_REGULADA`)

| Cor        | Prioridade | Tempo máximo de espera |
|------------|-----------|------------------------|
| `VERMELHO` | 1 — urgente | 1 mês                 |
| `AMARELO`  | 2          | 3 meses                |
| `VERDE`    | 3          | 6 meses                |
| `AZUL`     | 4 — rotina | 1 ano                  |

### Critério 3 — Grupo de prioridade legal (somente `FILA_REGULADA`, mesmo cor)

Baseado na Lei 10.048/2000 e no Estatuto do Idoso. Pacientes com **60 anos ou mais recebem o grupo `IDOSO` automaticamente**, sobrescrevendo o campo `grupoLegal` cadastrado.

| Grupo        | Prioridade |
|--------------|-----------|
| `IDOSO`      | 1         |
| `GESTANTE`   | 2         |
| `DEFICIENTE` | 3         |
| `LACTANTE`   | 4         |
| `OBESO`      | 5         |
| `GERAL`      | 6         |

### Critério 4 — Timestamp de chegada (desempate)

Quando todos os critérios anteriores empatam, quem entrou na fila primeiro tem prioridade. É o único critério aplicado dentro de `FILA_ESPERA`.

### Exemplo

| Paciente         | Tipo           | Cor       | Grupo  | Chegada     | Posição | Motivo                              |
|------------------|----------------|-----------|--------|-------------|---------|-------------------------------------|
| Ana              | FILA_REGULADA  | VERMELHO  | GERAL  | 05/01 10h   | **1°**  | Cor mais urgente                    |
| Pedro (65 anos)  | FILA_REGULADA  | AMARELO   | IDOSO* | 03/01 09h   | **2°**  | Idoso automático (≥60)              |
| Joana            | FILA_REGULADA  | AMARELO   | GERAL  | 02/01 07h   | **3°**  | Mesma cor, grupo menor              |
| Carlos (72 anos) | FILA_ESPERA    | AZUL      | IDOSO  | 01/01 08h   | **4°**  | FILA_ESPERA — apenas timestamp      |
| Marcos           | FILA_ESPERA    | AZUL      | GERAL  | 04/01 11h   | **5°**  | FILA_ESPERA — chegou depois         |

> *Pedro tem `grupoLegal = GERAL`, mas `PriorityCalculator.getPriorityGroup()` retorna `IDOSO` por ter 65 anos.

---

## Variáveis de Ambiente

### queue-service

| Variável                   | Padrão (local)                              | Descrição                          |
|----------------------------|---------------------------------------------|------------------------------------|
| `JWT_SECRET`               | —                                           | **Obrigatório.** Chave HMAC-SHA256 |
| `SPRING_DATASOURCE_URL`    | `jdbc:postgresql://localhost:5432/sus_queue_db` | URL do PostgreSQL              |
| `SPRING_DATASOURCE_USERNAME` | `sus_user`                                | Usuário do banco                   |
| `SPRING_DATASOURCE_PASSWORD` | `sus_pass`                                | Senha do banco                     |
| `RABBITMQ_HOST`            | `localhost`                                 | Host do RabbitMQ                   |
| `RABBITMQ_PORT`            | `5672`                                      | Porta AMQP                         |
| `RABBITMQ_USERNAME`        | `admin`                                     | Usuário RabbitMQ                   |
| `RABBITMQ_PASSWORD`        | `admin`                                     | Senha RabbitMQ                     |

### auth-service

| Variável           | Padrão    | Descrição                                    |
|--------------------|-----------|----------------------------------------------|
| `JWT_SECRET`       | —         | **Obrigatório.** Deve ser igual ao do queue-service |
| `JWT_EXPIRATION_MS`| `86400000`| Expiração do token em ms (padrão: 24h)       |

---

## Banco de Dados

Um único PostgreSQL com dois schemas isolados (gerenciados pelo Flyway):

- `queue` — tabelas do queue-service (`patients`, `queue_entries`, `procedures`, `patient_procedures`)
- `auth` — tabelas do auth-service (`users`)

O script `init.sql` cria os schemas e concede permissões ao usuário `sus_user`.

---

## Documentação

| Documento                                                     | Conteúdo                                                           |
|---------------------------------------------------------------|--------------------------------------------------------------------|
| [Arquitetura do Sistema](docs/arquitetura-sistema-sus.md)     | Visão detalhada dos microsserviços, fluxos e decisões de design    |
| [Lógica de Priorização](docs/logica_priorizacao_sus.md)       | Algoritmo completo do `PriorityCalculator` com exemplos            |
| [Contrato de API](docs/api-contract.md)                       | Especificação dos endpoints, payloads e códigos HTTP               |
| [Diagramas de Sequência](docs/sequencias-diagrama.md)         | Fluxos de chamar próximo, registrar, reclassificar e muito mais    |
| [Modelo de Dados (ERD)](docs/erd.md)                          | Diagrama entidade-relacionamento do banco de dados                 |
| [Requisitos e Domínio SUS](docs/requisitos-dominio-sus.md)    | Protocolo de cores, grupos legais, legislação e fluxo SISREG       |
