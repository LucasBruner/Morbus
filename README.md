# Morbus

Sistema de gerenciamento de filas ambulatoriais do SUS — FIAP PosTech Fase 5 Hackathon.

## Arquitetura

Três microserviços independentes:

| Serviço | Porta | Responsabilidade |
|---|---|---|
| `auth-service` | 8082 | Autenticação e emissão de JWT |
| `queue-service` | 8080 | Gerenciamento de filas e pacientes |
| `notification-service` | 8081 | Notificações via RabbitMQ |

## Pré-requisitos

- Docker e Docker Compose
- Java 21 (para desenvolvimento local)
- Maven 3.9+

## Executando com Docker Compose

### 1. Configurar variáveis de ambiente

Crie um arquivo `.env` na raiz do projeto (ou exporte as variáveis no shell):

```env
# Segredo usado para assinar e validar tokens JWT.
# OBRIGATÓRIO. Deve ser uma string aleatória de pelo menos 32 caracteres.
# Deve ser IDÊNTICO no auth-service e no queue-service.
JWT_SECRET=sua-chave-secreta-longa-e-aleatoria-aqui
```

> ⚠️ **Nunca commite o `.env` com valores reais.** O arquivo `.env` está no `.gitignore`.
> Use `openssl rand -base64 32` para gerar uma chave segura.

### 2. Subir os serviços

```bash
docker compose up --build
```

### 3. Acessar

| Recurso | URL |
|---|---|
| Queue Service — Swagger UI | http://localhost:8080/swagger-ui.html |
| Auth Service — Swagger UI | http://localhost:8082/swagger-ui.html |
| RabbitMQ Management | http://localhost:15672 (admin/admin) |

## Autenticação

Todos os endpoints protegidos exigem um **Bearer token JWT** emitido pelo `auth-service`.

### Fluxo

1. Crie um usuário via `POST /api/v1/auth/register` no auth-service
2. Faça login via `POST /api/v1/auth/login` → receba o `token`
3. No Swagger UI do queue-service, clique em **Authorize** e informe `Bearer <token>`

### Roles

| Role | Endpoints liberados |
|---|---|
| `MEDICO` | `POST /api/v1/queue`, `GET /api/v1/queue`, `POST /api/v1/queue/call-next`, `PATCH /api/v1/queue/{id}/priority`, `DELETE /api/v1/queue/{id}`, `GET /api/v1/patients/**`, `GET /api/v1/procedures/**` |
| `PACIENTE` | `GET /api/v1/queue/{id}/position`, `GET /api/v1/procedures/**` |

Tentativa de acesso sem permissão retorna `403` com body:
```json
{ "error": "Acesso negado: perfil sem permissao para esta operacao" }
```

Token ausente ou inválido retorna `401`:
```json
{ "error": "Token invalido ou ausente" }
```

## Variáveis de Ambiente

### queue-service

| Variável | Padrão (local) | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/sus_queue_db` | URL do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `sus_user` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | `sus_pass` | Senha do banco |
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Porta AMQP |
| `RABBITMQ_USERNAME` | `admin` | Usuário RabbitMQ |
| `RABBITMQ_PASSWORD` | `admin` | Senha RabbitMQ |
| `JWT_SECRET` | — | **Obrigatório.** Chave HMAC para validar tokens JWT |

### auth-service

| Variável | Padrão | Descrição |
|---|---|---|
| `JWT_SECRET` | — | **Obrigatório.** Deve ser igual ao do queue-service |
| `JWT_EXPIRATION_MS` | `86400000` | Expiração do token em ms (padrão: 24h) |

## Banco de Dados

Um único PostgreSQL com dois schemas isolados:

- `queue` — tabelas do queue-service (Flyway gerenciado)
- `auth` — tabelas do auth-service (Flyway gerenciado)

O script `init.sql` cria os schemas e concede permissões ao usuário `sus_user`.
