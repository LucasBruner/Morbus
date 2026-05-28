# Contrato de API — Sistema de Fila SUS
> Hackathon FIAP PosTech — Arquitetura e Desenvolvimento Java — Fase 5

---

## Convenções

- Todos os endpoints retornam `Content-Type: application/json`
- Datas e timestamps seguem o formato **ISO 8601**: `yyyy-MM-dd` / `yyyy-MM-ddTHH:mm:ssZ`
- IDs são **UUID v4**
- Endpoints protegidos exigem o header: `Authorization: Bearer <jwt_token>`
- Erros seguem um envelope padrão (ver seção [Respostas de Erro](#respostas-de-erro))

---

## Índice

- [auth-service `:8082`](#auth-service-8082)
  - [POST /auth/register](#post-authregister)
  - [POST /auth/login](#post-authlogin)
- [queue-service `:8080`](#queue-service-8080)
  - [POST /api/v1/queue](#post-apiv1queue)
  - [GET /api/v1/queue](#get-apiv1queue)
  - [GET /api/v1/queue/{id}/position](#get-apiv1queueidposition)
  - [POST /api/v1/queue/call-next](#post-apiv1queuecall-next)
  - [PATCH /api/v1/queue/{id}/priority](#patch-apiv1queueidpriority)
  - [DELETE /api/v1/queue/{id}](#delete-apiv1queueid)
  - [POST /api/v1/patients](#post-apiv1patients)
  - [GET /api/v1/patients/{id}](#get-apiv1patientsid)
  - [GET /api/v1/patients?cpf={cpf}](#get-apiv1patientscpfcpf)
  - [GET /api/v1/procedures](#get-apiv1procedures)
  - [GET /api/v1/procedures/{id}](#get-apiv1proceduresid)
- [notification-service `:8081`](#notification-service-8081)
  - [GET /api/v1/notifications](#get-apiv1notifications)
  - [GET /api/v1/notifications/{id}](#get-apiv1notificationsid)
- [Enumeradores](#enumeradores)
- [Respostas de Erro](#respostas-de-erro)

---

## auth-service `:8082`

### POST /auth/register

Cria um novo usuário com role MEDICO ou PACIENTE.

**Auth:** ❌ público

**Request Body:**
```json
{
  "username": "dr.silva",
  "email": "silva@sus.gov.br",
  "password": "Senh@Forte123",
  "role": "MEDICO"
}
```

| Campo      | Tipo   | Obrigatório | Validações                         |
|------------|--------|-------------|-------------------------------------|
| `username` | string | ✅          | 3–100 caracteres, único             |
| `email`    | string | ✅          | Formato e-mail válido, único        |
| `password` | string | ✅          | Mínimo 8 caracteres                 |
| `role`     | enum   | ✅          | `MEDICO` \| `PACIENTE`              |

**Response `201 Created`:**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "username": "dr.silva",
  "email": "silva@sus.gov.br",
  "role": "MEDICO",
  "createdAt": "2026-05-27T10:00:00Z"
}
```

**Erros:**

| Status | Motivo                           |
|--------|----------------------------------|
| `400`  | Campos inválidos ou ausentes     |
| `409`  | Username ou e-mail já cadastrado |

---

### POST /auth/login

Autentica o usuário e retorna o token JWT.

**Auth:** ❌ público

**Request Body:**
```json
{
  "username": "dr.silva",
  "password": "Senh@Forte123"
}
```

| Campo      | Tipo   | Obrigatório |
|------------|--------|-------------|
| `username` | string | ✅          |
| `password` | string | ✅          |

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkci5zaWx2YSIsInJvbGUiOiJST0xFX01FRElDTyIsImlhdCI6MTc0ODMzNjAwMCwiZXhwIjoxNzQ4NDIyNDAwfQ.assinatura",
  "type": "Bearer",
  "expiresIn": 86400000,
  "role": "ROLE_MEDICO"
}
```

| Campo       | Tipo   | Descrição                                    |
|-------------|--------|----------------------------------------------|
| `token`     | string | JWT assinado com HMAC-SHA256                 |
| `type`      | string | Sempre `"Bearer"`                            |
| `expiresIn` | number | Validade em milissegundos (padrão: 24h)      |
| `role`      | string | `ROLE_MEDICO` \| `ROLE_PACIENTE`             |

**JWT Payload (decodificado):**
```json
{
  "sub": "dr.silva",
  "role": "ROLE_MEDICO",
  "iat": 1748336000,
  "exp": 1748422400
}
```

**Erros:**

| Status | Motivo                       |
|--------|------------------------------|
| `400`  | Campos ausentes              |
| `401`  | Credenciais inválidas        |

---

## queue-service `:8080`

> Todos os endpoints abaixo exigem o header:
> ```
> Authorization: Bearer <token>
> ```

---

### POST /api/v1/queue

Cadastra um paciente em uma fila de procedimento. A cor de entrada é sempre **AZUL** e o grupo legal é detectado automaticamente pela data de nascimento e campos do paciente.

**Auth:** `ROLE_MEDICO`

**Request Body:**
```json
{
  "patientId": "f1e2d3c4-b5a6-7890-fedc-ba0987654321",
  "procedureId": "c0d1e2f3-a4b5-6789-cdef-012345678901"
}
```

| Campo         | Tipo | Obrigatório | Descrição                     |
|---------------|------|-------------|-------------------------------|
| `patientId`   | UUID | ✅          | ID do paciente já cadastrado  |
| `procedureId` | UUID | ✅          | ID do procedimento solicitado |

**Response `201 Created`:**
```json
{
  "id": "e5f6a7b8-c9d0-1234-ef56-789012345678",
  "patient": {
    "id": "f1e2d3c4-b5a6-7890-fedc-ba0987654321",
    "nomeCompleto": "João da Silva",
    "cpf": "123.456.789-00"
  },
  "procedure": {
    "id": "c0d1e2f3-a4b5-6789-cdef-012345678901",
    "coProcedimento": "0301010072",
    "noProcedimento": "Consulta de Cardiologia"
  },
  "riskColor": "AZUL",
  "priorityGroup": "GERAL",
  "status": "AGUARDANDO",
  "position": 7,
  "registeredAt": "2026-05-27T10:15:00Z"
}
```

**Erros:**

| Status | Motivo                                               |
|--------|------------------------------------------------------|
| `404`  | Paciente ou procedimento não encontrado              |
| `422`  | Paciente fora da faixa etária do procedimento        |
| `409`  | Paciente já possui entrada ativa neste procedimento  |

---

### GET /api/v1/queue

Lista a fila ordenada por prioridade com suporte a filtros e paginação.

**Auth:** `ROLE_MEDICO`

**Query Parameters:**

| Parâmetro   | Tipo    | Obrigatório | Padrão      | Descrição                                |
|-------------|---------|-------------|-------------|------------------------------------------|
| `status`    | enum    | ❌          | `AGUARDANDO`| Filtrar por status da entrada            |
| `riskColor` | enum    | ❌          | —           | Filtrar por cor de risco                 |
| `page`      | integer | ❌          | `0`         | Número da página (zero-based)            |
| `size`      | integer | ❌          | `20`        | Itens por página (máx. 100)              |

**Exemplo:** `GET /api/v1/queue?status=AGUARDANDO&riskColor=VERMELHO&page=0&size=10`

**Response `200 OK`:**
```json
{
  "content": [
    {
      "id": "e5f6a7b8-c9d0-1234-ef56-789012345678",
      "position": 1,
      "patient": {
        "id": "f1e2d3c4-b5a6-7890-fedc-ba0987654321",
        "nomeCompleto": "Maria Oliveira",
        "cpf": "987.654.321-00",
        "grupoLegal": "IDOSO"
      },
      "procedure": {
        "id": "c0d1e2f3-a4b5-6789-cdef-012345678901",
        "coProcedimento": "0301010072",
        "noProcedimento": "Consulta de Cardiologia"
      },
      "riskColor": "VERMELHO",
      "priorityGroup": "IDOSO",
      "status": "AGUARDANDO",
      "registeredAt": "2026-05-20T08:00:00Z",
      "updatedAt": null
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### GET /api/v1/queue/{id}/position

Retorna a posição atual de um paciente na fila e informações do seu cadastro.

**Auth:** `ROLE_MEDICO` ou `ROLE_PACIENTE`

**Path Parameters:**

| Parâmetro | Tipo | Descrição            |
|-----------|------|----------------------|
| `id`      | UUID | ID da entrada na fila|

**Response `200 OK`:**
```json
{
  "id": "e5f6a7b8-c9d0-1234-ef56-789012345678",
  "position": 3,
  "totalAhead": 2,
  "patient": {
    "id": "f1e2d3c4-b5a6-7890-fedc-ba0987654321",
    "nomeCompleto": "João da Silva"
  },
  "procedure": {
    "coProcedimento": "0301010072",
    "noProcedimento": "Consulta de Cardiologia"
  },
  "riskColor": "AZUL",
  "priorityGroup": "GERAL",
  "status": "AGUARDANDO",
  "estimatedWaitMonths": 12,
  "registeredAt": "2026-05-27T10:15:00Z"
}
```

| Campo                | Tipo    | Descrição                                              |
|----------------------|---------|--------------------------------------------------------|
| `position`           | integer | Posição atual na fila (1 = próximo a ser chamado)      |
| `totalAhead`         | integer | Quantas entradas têm prioridade maior                  |
| `estimatedWaitMonths`| integer | Estimativa em meses baseada na cor: AZUL=12, VERDE=6, AMARELO=3, VERMELHO=1 |

**Erros:**

| Status | Motivo                        |
|--------|-------------------------------|
| `404`  | Entrada na fila não encontrada|

---

### POST /api/v1/queue/call-next

Chama o próximo paciente da fila (maior prioridade com status `AGUARDANDO`). Altera o status para `AGENDADO` e publica o evento `PATIENT_CALLED`.

**Auth:** `ROLE_MEDICO`

**Request Body:** nenhum

**Response `200 OK`:**
```json
{
  "id": "e5f6a7b8-c9d0-1234-ef56-789012345678",
  "patient": {
    "id": "f1e2d3c4-b5a6-7890-fedc-ba0987654321",
    "nomeCompleto": "Maria Oliveira",
    "cpf": "987.654.321-00",
    "contato": "maria@email.com"
  },
  "procedure": {
    "coProcedimento": "0301010072",
    "noProcedimento": "Consulta de Cardiologia"
  },
  "riskColor": "VERMELHO",
  "priorityGroup": "IDOSO",
  "status": "AGENDADO",
  "calledAt": "2026-05-27T14:00:00Z"
}
```

**Erros:**

| Status | Motivo                                           |
|--------|--------------------------------------------------|
| `404`  | Nenhum paciente aguardando na fila               |

---

### PATCH /api/v1/queue/{id}/priority

Reclassifica a cor de risco de uma entrada na fila. Publica o evento `PRIORITY_UPDATED`.

**Auth:** `ROLE_MEDICO`

**Path Parameters:**

| Parâmetro | Tipo | Descrição            |
|-----------|------|----------------------|
| `id`      | UUID | ID da entrada na fila|

**Request Body:**
```json
{
  "riskColor": "AMARELO"
}
```

| Campo       | Tipo | Obrigatório | Valores aceitos                          |
|-------------|------|-------------|------------------------------------------|
| `riskColor` | enum | ✅          | `VERMELHO` \| `AMARELO` \| `VERDE` \| `AZUL` |

**Response `200 OK`:**
```json
{
  "id": "e5f6a7b8-c9d0-1234-ef56-789012345678",
  "riskColor": "AMARELO",
  "priorityGroup": "GERAL",
  "status": "AGUARDANDO",
  "newPosition": 4,
  "updatedAt": "2026-05-27T14:30:00Z"
}
```

**Erros:**

| Status | Motivo                                                    |
|--------|-----------------------------------------------------------|
| `404`  | Entrada não encontrada                                    |
| `422`  | Status da entrada não permite reclassificação (ex: `ATENDIDO`) |

---

### DELETE /api/v1/queue/{id}

Cancela a entrada de um paciente na fila. Publica o evento `PATIENT_CANCELLED`.

**Auth:** `ROLE_MEDICO`

**Path Parameters:**

| Parâmetro | Tipo | Descrição            |
|-----------|------|----------------------|
| `id`      | UUID | ID da entrada na fila|

**Query Parameters:**

| Parâmetro | Tipo   | Obrigatório | Descrição             |
|-----------|--------|-------------|-----------------------|
| `motivo`  | string | ❌          | Motivo do cancelamento|

**Response `204 No Content`**

**Erros:**

| Status | Motivo                                                              |
|--------|---------------------------------------------------------------------|
| `404`  | Entrada não encontrada                                              |
| `422`  | Status não permite cancelamento (ex: entrada já `ATENDIDO`)        |

---

### POST /api/v1/patients

Cadastra um novo paciente no sistema.

**Auth:** `ROLE_MEDICO`

**Request Body:**
```json
{
  "cpf": "123.456.789-00",
  "cns": "700 0000 0000 0001",
  "nomeCompleto": "João da Silva",
  "dataNascimento": "1960-03-15",
  "sexo": "M",
  "contato": "joao@email.com",
  "gestante": false,
  "deficiente": false,
  "lactante": false,
  "obeso": false
}
```

| Campo            | Tipo    | Obrigatório | Validações                                      |
|------------------|---------|-------------|-------------------------------------------------|
| `cpf`            | string  | ✅          | Formato `###.###.###-##`, válido e único         |
| `cns`            | string  | ❌          | Cartão Nacional de Saúde, único                 |
| `nomeCompleto`   | string  | ✅          | 3–255 caracteres                                |
| `dataNascimento` | date    | ✅          | Formato `yyyy-MM-dd`, não pode ser no futuro    |
| `sexo`           | string  | ❌          | `M` \| `F` \| `O`                              |
| `contato`        | string  | ❌          | E-mail ou telefone para notificações            |
| `gestante`       | boolean | ❌          | Padrão `false`                                  |
| `deficiente`     | boolean | ❌          | Padrão `false`                                  |
| `lactante`       | boolean | ❌          | Padrão `false`                                  |
| `obeso`          | boolean | ❌          | Padrão `false`                                  |

> **Detecção automática de grupo legal:** o sistema calcula o `grupoLegal` automaticamente. Paciente com 60+ anos é classificado como `IDOSO` independentemente dos campos booleanos. A prioridade de detecção é: `IDOSO > GESTANTE > DEFICIENTE > LACTANTE > OBESO > GERAL`.

**Response `201 Created`:**
```json
{
  "id": "f1e2d3c4-b5a6-7890-fedc-ba0987654321",
  "cpf": "123.456.789-00",
  "cns": "700 0000 0000 0001",
  "nomeCompleto": "João da Silva",
  "dataNascimento": "1960-03-15",
  "sexo": "M",
  "contato": "joao@email.com",
  "grupoLegal": "IDOSO",
  "createdAt": "2026-05-27T10:00:00Z"
}
```

**Erros:**

| Status | Motivo                         |
|--------|--------------------------------|
| `400`  | Campos inválidos ou ausentes   |
| `409`  | CPF ou CNS já cadastrado       |

---

### GET /api/v1/patients/{id}

Busca um paciente pelo seu ID.

**Auth:** `ROLE_MEDICO`

**Path Parameters:**

| Parâmetro | Tipo | Descrição    |
|-----------|------|--------------|
| `id`      | UUID | ID do paciente|

**Response `200 OK`:**
```json
{
  "id": "f1e2d3c4-b5a6-7890-fedc-ba0987654321",
  "cpf": "123.456.789-00",
  "cns": "700 0000 0000 0001",
  "nomeCompleto": "João da Silva",
  "dataNascimento": "1960-03-15",
  "sexo": "M",
  "contato": "joao@email.com",
  "grupoLegal": "IDOSO",
  "createdAt": "2026-05-27T10:00:00Z"
}
```

**Erros:**

| Status | Motivo                    |
|--------|---------------------------|
| `404`  | Paciente não encontrado   |

---

### GET /api/v1/patients?cpf={cpf}

Busca um paciente pelo CPF.

**Auth:** `ROLE_MEDICO`

**Query Parameters:**

| Parâmetro | Tipo   | Obrigatório | Exemplo            |
|-----------|--------|-------------|--------------------|
| `cpf`     | string | ✅          | `123.456.789-00`   |

**Exemplo:** `GET /api/v1/patients?cpf=123.456.789-00`

**Response `200 OK`:** mesmo schema de `GET /api/v1/patients/{id}`

**Erros:**

| Status | Motivo                    |
|--------|---------------------------|
| `404`  | Paciente não encontrado   |

---

### GET /api/v1/procedures

Lista os procedimentos disponíveis para agendamento na fila, com suporte a paginação.

**Auth:** `ROLE_MEDICO` ou `ROLE_PACIENTE`

**Query Parameters:**

| Parâmetro | Tipo    | Obrigatório | Padrão | Descrição                         |
|-----------|---------|-------------|--------|-----------------------------------|
| `page`    | integer | ❌          | `0`    | Número da página (zero-based)     |
| `size`    | integer | ❌          | `20`   | Itens por página (máx. 100)       |
| `codigo`  | string  | ❌          | —      | Filtrar por código SIGTAP (parcial)|

**Response `200 OK`:**
```json
{
  "content": [
    {
      "id": "c0d1e2f3-a4b5-6789-cdef-012345678901",
      "coProcedimento": "0301010072",
      "noProcedimento": "CONSULTA MEDICA EM ATENCAO ESPECIALIZADA",
      "idadeMinima": 0,
      "idadeMaxima": 120,
      "grupo": "CONSULTAS / ATENDIMENTOS / ACOMPANHAMENTOS"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 20,
  "totalPages": 1
}
```

---

### GET /api/v1/procedures/{id}

Busca um procedimento pelo ID.

**Auth:** `ROLE_MEDICO` ou `ROLE_PACIENTE`

**Path Parameters:**

| Parâmetro | Tipo | Descrição        |
|-----------|------|------------------|
| `id`      | UUID | ID do procedimento|

**Response `200 OK`:**
```json
{
  "id": "c0d1e2f3-a4b5-6789-cdef-012345678901",
  "coProcedimento": "0301010072",
  "noProcedimento": "CONSULTA MEDICA EM ATENCAO ESPECIALIZADA",
  "idadeMinima": 0,
  "idadeMaxima": 120,
  "grupo": "CONSULTAS / ATENDIMENTOS / ACOMPANHAMENTOS"
}
```

**Erros:**

| Status | Motivo                       |
|--------|------------------------------|
| `404`  | Procedimento não encontrado  |

---

## notification-service `:8081`

> Os endpoints do notification-service não exigem autenticação JWT — são de consulta interna para fins de observabilidade e demo.

---

### GET /api/v1/notifications

Lista o histórico de notificações enviadas, ordenadas por `sentAt` decrescente.

**Auth:** ❌ público (interno)

**Query Parameters:**

| Parâmetro   | Tipo    | Obrigatório | Padrão | Descrição                                          |
|-------------|---------|-------------|--------|----------------------------------------------------|
| `eventType` | enum    | ❌          | —      | Filtrar por tipo: `PATIENT_REGISTERED`, `PATIENT_CALLED`, `PRIORITY_UPDATED`, `PATIENT_CANCELLED` |
| `page`      | integer | ❌          | `0`    | Número da página (zero-based)                      |
| `size`      | integer | ❌          | `20`   | Itens por página                                   |

**Response `200 OK`:**
```json
{
  "content": [
    {
      "id": "b2c3d4e5-f6a7-8901-bcde-f01234567890",
      "eventType": "PATIENT_CALLED",
      "recipientName": "Maria Oliveira",
      "recipientContact": "maria@email.com",
      "message": "É a sua vez! Compareça ao guichê para CONSULTA MEDICA EM ATENCAO ESPECIALIZADA.",
      "sentAt": "2026-05-27T14:00:05Z",
      "status": "SENT"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

| Campo              | Tipo   | Descrição                                 |
|--------------------|--------|-------------------------------------------|
| `eventType`        | enum   | Tipo do evento que originou a notificação |
| `recipientName`    | string | Nome do paciente notificado               |
| `recipientContact` | string | E-mail ou telefone                        |
| `message`          | string | Mensagem enviada ao paciente              |
| `status`           | enum   | `SENT` \| `FAILED`                       |

---

### GET /api/v1/notifications/{id}

Busca uma notificação específica pelo ID.

**Auth:** ❌ público (interno)

**Path Parameters:**

| Parâmetro | Tipo | Descrição          |
|-----------|------|--------------------|
| `id`      | UUID | ID da notificação  |

**Response `200 OK`:** mesmo schema do item individual acima

**Erros:**

| Status | Motivo                         |
|--------|--------------------------------|
| `404`  | Notificação não encontrada     |

---

## Enumeradores

### RiskColor

| Valor      | Prioridade | Tempo máximo de espera |
|------------|------------|------------------------|
| `VERMELHO` | 1 (maior)  | 1 mês                  |
| `AMARELO`  | 2          | 3 meses                |
| `VERDE`    | 3          | 6 meses                |
| `AZUL`     | 4 (menor)  | 1 ano (entrada padrão) |

### PriorityGroup / GrupoLegal

| Valor        | Prioridade | Critério de elegibilidade       |
|--------------|------------|---------------------------------|
| `IDOSO`      | 1 (maior)  | Idade ≥ 60 anos                 |
| `GESTANTE`   | 2          | Campo `gestante: true`          |
| `DEFICIENTE` | 3          | Campo `deficiente: true`        |
| `LACTANTE`   | 4          | Campo `lactante: true`          |
| `OBESO`      | 5          | Campo `obeso: true`             |
| `GERAL`      | 6 (menor)  | Padrão — nenhum critério        |

### QueueStatus

| Valor        | Descrição                                               |
|--------------|---------------------------------------------------------|
| `AGUARDANDO` | Paciente aguardando chamada (estado inicial)            |
| `AGENDADO`   | Paciente chamado, aguardando comparecimento             |
| `ATENDIDO`   | Atendimento realizado                                   |
| `FALTOU`     | Paciente não compareceu após ser chamado                |
| `CANCELADO`  | Entrada cancelada (pelo médico ou pelo paciente)        |
| `DEVOLVIDO`  | Paciente devolvido à fila após não comparecimento       |

### NotificationType

| Valor                | Contexto                                      |
|----------------------|-----------------------------------------------|
| `PATIENT_REGISTERED` | Paciente adicionado à fila                    |
| `PATIENT_CALLED`     | Paciente chamado para atendimento             |
| `PRIORITY_UPDATED`   | Cor de risco do paciente foi alterada         |
| `PATIENT_CANCELLED`  | Entrada do paciente foi cancelada             |

### UserRole

| Valor      | Prefixo Spring Security | Permissões                               |
|------------|-------------------------|------------------------------------------|
| `MEDICO`   | `ROLE_MEDICO`           | Gestão completa da fila e pacientes      |
| `PACIENTE` | `ROLE_PACIENTE`         | Consulta de posição e procedimentos      |

---

## Respostas de Erro

Todos os erros retornam o seguinte envelope padrão:

**`400 Bad Request` — Validação:**
```json
{
  "status": 400,
  "error": "Requisição inválida",
  "messages": [
    "cpf: CPF inválido",
    "dataNascimento: não pode ser no futuro"
  ],
  "timestamp": "2026-05-27T10:00:00Z",
  "path": "/api/v1/patients"
}
```

**`401 Unauthorized` — Token ausente ou inválido:**
```json
{
  "status": 401,
  "error": "Token inválido ou expirado",
  "timestamp": "2026-05-27T10:00:00Z",
  "path": "/api/v1/queue"
}
```

**`403 Forbidden` — Role sem permissão:**
```json
{
  "status": 403,
  "error": "Acesso negado: perfil sem permissão para esta operação",
  "timestamp": "2026-05-27T10:00:00Z",
  "path": "/api/v1/queue/call-next"
}
```

**`404 Not Found`:**
```json
{
  "status": 404,
  "error": "Recurso não encontrado",
  "message": "QueueEntry com id 'e5f6a7b8...' não encontrado",
  "timestamp": "2026-05-27T10:00:00Z",
  "path": "/api/v1/queue/e5f6a7b8-c9d0-1234-ef56-789012345678/position"
}
```

**`409 Conflict`:**
```json
{
  "status": 409,
  "error": "Conflito de dados",
  "message": "CPF '123.456.789-00' já cadastrado no sistema",
  "timestamp": "2026-05-27T10:00:00Z",
  "path": "/api/v1/patients"
}
```

**`422 Unprocessable Entity` — Regra de negócio violada:**
```json
{
  "status": 422,
  "error": "Regra de negócio violada",
  "message": "Paciente não elegível: procedimento exige idade mínima de 18 anos",
  "timestamp": "2026-05-27T10:00:00Z",
  "path": "/api/v1/queue"
}
```

---

*Documento gerado em: maio/2026*
