# Contrato de API — Sistema de Fila SUS
> Hackathon FIAP PosTech — Arquitetura e Desenvolvimento Java — Fase 5

---

## Convenções

- Todos os endpoints retornam `Content-Type: application/json`
- Datas e timestamps seguem o formato **ISO 8601**: `yyyy-MM-dd` / `yyyy-MM-ddTHH:mm:ssZ`
- IDs são **UUID v4**, exceto no notification-service, que usa identificador numérico sequencial (`Long`) — serviço interno de auditoria/observabilidade sem consumidores externos que dependam do formato UUID
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
  - [PATCH /api/v1/patients/{id}](#patch-apiv1patientsid)
  - [PATCH /api/v1/patients/{id}/inactivate](#patch-apiv1patientsidiactivate)
  - [POST /api/v1/patients/{patientId}/procedures/{procedureId}](#post-apiv1patientspatientidproceduresprocedureid)
  - [DELETE /api/v1/patients/{patientId}/procedures/{procedureId}](#delete-apiv1patientspatientidproceduresprocedureid)
  - [GET /api/v1/procedures](#get-apiv1procedures)
  - [GET /api/v1/procedures?codigo={codigo}](#get-apiv1procedurescodigocodigo)
  - [GET /api/v1/procedures/{id}](#get-apiv1proceduresid)
- [notification-service `:8081`](#notification-service-8081)
  - [GET /api/v1/notifications](#get-apiv1notifications)
  - [GET /api/v1/notifications/{id}](#get-apiv1notificationsid)
- [regulacao-service `:8083`](#regulacao-service-8083)
  - [POST /api/v1/solicitacoes](#post-apiv1solicitacoes)
  - [GET /api/v1/solicitacoes](#get-apiv1solicitacoes)
  - [GET /api/v1/solicitacoes/{id}](#get-apiv1solicitacoesid)
  - [POST /api/v1/solicitacoes/{id}/complementar](#post-apiv1solicitacoesidcomplementar)
  - [DELETE /api/v1/solicitacoes/{id}](#delete-apiv1solicitacoesid)
  - [POST /api/v1/regulacao/{id}/avaliar](#post-apiv1regulacaoiavaliar)
  - [PATCH /api/v1/regulacao/solicitacoes/{id}/risco](#patch-apiv1regulacaosolicitacoesidrisco)
  - [GET /api/v1/regulacao/pendentes](#get-apiv1regulacaopendentes)
  - [GET /api/v1/regulacao/pendentes-vaga](#get-apiv1regulacaopendentes-vaga)
- [agendamento-service `:8084`](#agendamento-service-8084)
  - [REST — Commands](#rest--commands)
  - [GraphQL — Queries](#graphql--queries)
- [Enumeradores](#enumeradores)
- [Respostas de Erro](#respostas-de-erro)

---

## auth-service `:8082`

### POST /auth/register

Cria um novo usuário com a role desejada.

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

| Campo      | Tipo   | Obrigatório | Validações                                                                 |
|------------|--------|-------------|----------------------------------------------------------------------------|
| `username` | string | ✅          | 3–100 caracteres, único                                                    |
| `email`    | string | ✅          | Formato e-mail válido, único                                               |
| `password` | string | ✅          | Mínimo 9 caracteres, contendo ao menos 1 letra maiúscula, 1 minúscula, 1 dígito e 1 caractere especial |
| `role`     | enum   | ✅          | `MEDICO` \| `PACIENTE` \| `SOLICITANTE` \| `REGULADOR` \| `EXECUTANTE`    |

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
| `400`  | Campos inválidos ou ausentes (`type`: `.../problems/validation-error`)    |
| `409`  | Username ou e-mail já cadastrado (`type`: `.../problems/user-already-exists`) |
| `422`  | Senha não atende aos requisitos mínimos (`type`: `.../problems/invalid-password`) |

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
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkci5zaWx2YSIsInJvbGUiOiJNRURJQ08iLCJpYXQiOjE3NDgzMzYwMDAsImV4cCI6MTc0ODQyMjQwMH0.assinatura",
  "type": "Bearer",
  "expiresIn": 86400000,
  "role": "MEDICO"
}
```

| Campo       | Tipo   | Descrição                                    |
|-------------|--------|----------------------------------------------|
| `token`     | string | JWT assinado com HMAC-SHA256                 |
| `type`      | string | Sempre `"Bearer"`                            |
| `expiresIn` | number | Validade em milissegundos (padrão: 24h)      |
| `role`      | string | Nome puro do enum `UserRole` (sem prefixo `ROLE_`) — ex: `MEDICO` \| `PACIENTE`. Os demais serviços normalizam para `ROLE_<valor>` ao construir a authority Spring Security a partir do claim JWT. |

**JWT Payload (decodificado):**
```json
{
  "sub": "dr.silva",
  "role": "MEDICO",
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

> Entradas criadas via evento `SOLICITATION_APPROVED` do regulacao-service são inseridas automaticamente pelo consumer interno — não requerem chamada manual a este endpoint.

**Auth:** `ROLE_MEDICO`

**Request Body:**
```json
{
  "patientId": "f1e2d3c4-b5a6-7890-fedc-ba0987654321",
  "procedureId": "c0d1e2f3-a4b5-6789-cdef-012345678901",
  "tipoFila": "FILA_REGULADA",
  "preferredUnitId": null
}
```

| Campo             | Tipo   | Obrigatório | Descrição                                            |
|-------------------|--------|-------------|------------------------------------------------------|
| `patientId`       | UUID   | ✅          | ID do paciente já cadastrado                         |
| `procedureId`     | UUID   | ✅          | ID do procedimento solicitado                        |
| `tipoFila`        | enum   | ✅          | `FILA_REGULADA` \| `FILA_ESPERA`                     |
| `preferredUnitId` | UUID   | ❌          | Unidade preferida pelo paciente (nullable)           |

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

Lista a fila de um procedimento ordenada por prioridade (riskColor → grupoLegal → registeredAt).

**Auth:** `ROLE_MEDICO`

**Query Parameters:**

| Parâmetro     | Tipo    | Obrigatório | Padrão      | Descrição                                |
|---------------|---------|-------------|-------------|------------------------------------------|
| `procedureId` | UUID    | ✅          | —           | ID do procedimento para filtrar a fila   |
| `status`      | enum    | ❌          | —           | Filtrar por status da entrada            |
| `riskColor`   | enum    | ❌          | —           | Filtrar por cor de risco                 |
| `page`        | integer | ❌          | `0`         | Número da página (zero-based)            |
| `size`        | integer | ❌          | `20`        | Itens por página                         |

**Exemplo:** `GET /api/v1/queue?procedureId=c0d1e2f3-...&status=AGUARDANDO&riskColor=VERMELHO`

**Response `200 OK`:**
```json
[
  {
    "id": "e5f6a7b8-c9d0-1234-ef56-789012345678",
    "patient": {
      "id": "f1e2d3c4-b5a6-7890-fedc-ba0987654321",
      "nome": "Maria",
      "sobrenome": "Oliveira",
      "grupoLegal": "IDOSO"
    },
    "procedure": {
      "id": "c0d1e2f3-a4b5-6789-cdef-012345678901",
      "coProcedimento": "0301010072",
      "noProcedimento": "CONSULTA MEDICA EM ATENCAO ESPECIALIZADA"
    },
    "riskColor": "VERMELHO",
    "status": "AGUARDANDO",
    "registeredAt": "2026-05-20T08:00:00",
    "updatedAt": null
  }
]
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

Chama o próximo paciente da fila (maior prioridade com status `AGUARDANDO`). Altera o status para `CHAMADO` e publica o evento `PATIENT_CALLED`. O status `AGENDADO` é atribuído posteriormente pelo agendamento-service ao confirmar o slot.

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
  "tipoFila": "FILA_REGULADA",
  "status": "CHAMADO",
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
| `404`  | Entrada não encontrada                                                                                                        |
| `422`  | Status não permite reclassificação — apenas `AGUARDANDO` e `DEVOLVIDO` são permitidos                                        |
| `422`  | Tipo de fila não permite reclassificação — entradas em `FILA_ESPERA` não podem ter a cor alterada (sempre `AZUL`)             |

---

### DELETE /api/v1/queue/{id}

Cancela a entrada de um paciente na fila. Publica o evento `PATIENT_CANCELLED`.

**Auth:** `ROLE_MEDICO`

**Path Parameters:**

| Parâmetro | Tipo | Descrição            |
|-----------|------|----------------------|
| `id`      | UUID | ID da entrada na fila|

**Request Body:**
```json
{
  "motivoCancelamento": "Paciente transferido para outra unidade"
}
```

| Campo                | Tipo   | Obrigatório | Descrição             |
|----------------------|--------|-------------|-----------------------|
| `motivoCancelamento` | string | ✅          | Motivo do cancelamento|

**Response `204 No Content`**

**Erros:**

| Status | Motivo                                                                                            |
|--------|---------------------------------------------------------------------------------------------------|
| `404`  | Entrada não encontrada                                                                            |
| `422`  | Status não permite cancelamento — apenas `AGUARDANDO` e `AGENDADO` podem ser cancelados. `ATENDIDO`, `FALTOU`, `CANCELADO` e `DEVOLVIDO` retornam 422 |

---

### POST /api/v1/patients

Cadastra um novo paciente no sistema.

**Auth:** `ROLE_MEDICO`

**Request Body:**
```json
{
  "cpf": "123.456.789-09",
  "cns": "123456789012345",
  "nome": "João",
  "sobrenome": "da Silva",
  "dataNascimento": "1960-03-15",
  "gender": "MASCULINO",
  "contato": "joao@email.com",
  "grupoLegal": "GERAL"
}
```

| Campo            | Tipo   | Obrigatório | Validações                                              |
|------------------|--------|-------------|---------------------------------------------------------|
| `cpf`            | string | ✅          | Formato `###.###.###-##`, válido e único                |
| `cns`            | string | ❌          | Cartão Nacional de Saúde, único se informado            |
| `nome`           | string | ✅          | Primeiro nome do paciente                               |
| `sobrenome`      | string | ✅          | Sobrenome do paciente                                   |
| `dataNascimento` | date   | ✅          | Formato `yyyy-MM-dd`, não pode ser no futuro            |
| `gender`         | enum   | ❌          | `MASCULINO` \| `FEMININO` \| `OUTROS`                  |
| `contato`        | string | ❌          | E-mail ou telefone para notificações                    |
| `grupoLegal`     | enum   | ✅          | `IDOSO` \| `GESTANTE` \| `DEFICIENTE` \| `LACTANTE` \| `OBESO` \| `GERAL` |

> **Detecção automática de IDOSO:** paciente com 60+ anos recebe `grupoLegal = IDOSO` automaticamente via `PriorityCalculator.getPriorityGroup()`, sobrescrevendo o valor informado.

**Response `201 Created`:**
```json
{
  "id": "f1e2d3c4-b5a6-7890-fedc-ba0987654321",
  "cpf": "123.456.789-09",
  "cns": "123456789012345",
  "nome": "João",
  "sobrenome": "da Silva",
  "dataNascimento": "1960-03-15",
  "gender": "MASCULINO",
  "contato": "joao@email.com",
  "grupoLegal": "IDOSO",
  "ativo": true
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
  "cpf": "123.456.789-09",
  "cns": "123456789012345",
  "nome": "João",
  "sobrenome": "da Silva",
  "dataNascimento": "1960-03-15",
  "gender": "MASCULINO",
  "contato": "joao@email.com",
  "grupoLegal": "IDOSO",
  "ativo": true
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

### PATCH /api/v1/patients/{id}

Atualiza dados cadastrais de um paciente.

**Auth:** `ROLE_MEDICO`

**Path Parameters:**

| Parâmetro | Tipo | Descrição    |
|-----------|------|--------------|
| `id`      | UUID | ID do paciente|

**Request Body:**
```json
{
  "cns": "123456789012345",
  "nome": "João",
  "sobrenome": "da Silva",
  "dataNascimento": "1960-03-15",
  "gender": "MASCULINO",
  "contato": "joao@email.com",
  "grupoLegal": "GERAL"
}
```

**Response `200 OK`:** mesmo schema de `GET /api/v1/patients/{id}`

**Erros:**

| Status | Motivo                      |
|--------|-----------------------------|
| `400`  | Dados de entrada inválidos  |
| `404`  | Paciente não encontrado     |

---

### PATCH /api/v1/patients/{id}/inactivate

Inativa um paciente. Bloqueado se houver entradas ativas na fila.

**Auth:** `ROLE_MEDICO`

**Response `204 No Content`**

**Erros:**

| Status | Motivo                                            |
|--------|---------------------------------------------------|
| `404`  | Paciente não encontrado                           |
| `422`  | Paciente possui entradas ativas na fila           |

---

### POST /api/v1/patients/{patientId}/procedures/{procedureId}

Vincula um procedimento SUS a um paciente, verificando elegibilidade de idade e status ativo.

**Auth:** `ROLE_MEDICO`

**Path Parameters:**

| Parâmetro     | Tipo | Descrição         |
|---------------|------|-------------------|
| `patientId`   | UUID | ID do paciente    |
| `procedureId` | UUID | ID do procedimento|

**Response `200 OK`:** schema do procedimento vinculado (`id`, `coProcedimento`, `noProcedimento`, `idadeMinima`, `idadeMaxima`, `grupo`)

**Erros:**

| Status | Motivo                                                      |
|--------|-------------------------------------------------------------|
| `404`  | Paciente ou procedimento não encontrado                     |
| `409`  | Procedimento já vinculado ao paciente                       |
| `422`  | Paciente inativo ou fora da faixa etária do procedimento    |

---

### DELETE /api/v1/patients/{patientId}/procedures/{procedureId}

Desvincula um procedimento SUS do paciente. Bloqueado se houver entradas ativas na fila.

**Auth:** `ROLE_MEDICO`

**Response `204 No Content`**

**Erros:**

| Status | Motivo                                                      |
|--------|-------------------------------------------------------------|
| `404`  | Paciente ou procedimento não encontrado                     |
| `409`  | Existem entradas ativas na fila para este procedimento      |

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

### GET /api/v1/procedures?codigo={codigo}

Busca um procedimento pelo código SIGTAP.

**Auth:** `ROLE_MEDICO` ou `ROLE_PACIENTE`

**Query Parameters:**

| Parâmetro | Tipo   | Obrigatório | Exemplo        |
|-----------|--------|-------------|----------------|
| `codigo`  | string | ✅          | `0301010072`   |

**Exemplo:** `GET /api/v1/procedures?codigo=0301010072`

**Response `200 OK`:** mesmo schema de `GET /api/v1/procedures/{id}`

**Erros:**

| Status | Motivo                       |
|--------|------------------------------|
| `404`  | Procedimento não encontrado  |

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
      "id": 42,
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
| `id`      | Long | ID numérico da notificação (não é UUID — ver Convenções) |

**Response `200 OK`:** mesmo schema do item individual acima

**Erros:**

| Status | Motivo                         |
|--------|--------------------------------|
| `404`  | Notificação não encontrada     |

---

## regulacao-service `:8083`

> Todos os endpoints abaixo exigem o header:
> ```
> Authorization: Bearer <token>
> ```

---

### POST /api/v1/solicitacoes

Cria uma nova solicitação de inclusão de paciente em fila ambulatorial.

**Auth:** `ROLE_SOLICITANTE`

**Request Body:**
```json
{
  "patientId": "f1e2d3c4-b5a6-7890-fedc-ba0987654321",
  "procedureId": "c0d1e2f3-a4b5-6789-cdef-012345678901",
  "cid": "I10",
  "justificativaClinica": "Paciente com hipertensão grave sem controle ambulatorial.",
  "profissionalSolicitante": "Dr. Ana Costa",
  "crmProfissional": "CRM/SP 12345",
  "unitSolicitanteId": "aa11bb22-cc33-dd44-ee55-ff6677889900",
  "destino": "FILA_REGULADA"
}
```

| Campo                    | Tipo   | Obrigatório | Descrição                                |
|--------------------------|--------|-------------|------------------------------------------|
| `patientId`              | UUID   | ✅          | ID do paciente no queue-service          |
| `procedureId`            | UUID   | ✅          | ID do procedimento no queue-service      |
| `cid`                    | string | ✅          | Código CID-10 (default: `Z00.0`)         |
| `justificativaClinica`   | string | ✅          | Justificativa clínica detalhada          |
| `profissionalSolicitante`| string | ✅          | Nome do médico solicitante               |
| `crmProfissional`        | string | ❌          | CRM do profissional                      |
| `unitSolicitanteId`      | UUID   | ✅          | ID da unidade solicitante cadastrada     |
| `destino`                | enum   | ✅          | `FILA_REGULADA` \| `FILA_ESPERA`         |

**Response `201 Created`:**
```json
{
  "id": "11223344-5566-7788-99aa-bbccddeeff00",
  "patientId": "f1e2d3c4-b5a6-7890-fedc-ba0987654321",
  "procedureId": "c0d1e2f3-a4b5-6789-cdef-012345678901",
  "cid": "I10",
  "destino": "FILA_REGULADA",
  "riskColor": "AZUL",
  "status": "AGUARDANDO",
  "criadaEm": "2026-06-12T09:00:00Z"
}
```

> `riskColor` é sempre `AZUL` na criação — o construtor de `Solicitacao` define `riskColor = AZUL` explicitamente (não fica `null`). O regulador substitui essa cor pela definitiva ao avaliar (`AUTORIZAR`/`FILA_ESPERA`).

**Erros:**

| Status | Motivo                                                               |
|--------|----------------------------------------------------------------------|
| `404`  | Paciente, procedimento ou unidade não encontrados                    |
| `409`  | Solicitação duplicada — paciente já possui solicitação ativa para este procedimento |
| `422`  | Cota da UBS esgotada para FILA_ESPERA neste procedimento             |

---

### GET /api/v1/solicitacoes

Lista solicitações com filtros opcionais.

**Auth:** `ROLE_SOLICITANTE` (vê apenas da sua unidade) ou `ROLE_REGULADOR` (vê todas)

**Query Parameters:**

| Parâmetro    | Tipo    | Obrigatório | Descrição                           |
|--------------|---------|-------------|-------------------------------------|
| `unidadeId`  | UUID    | ❌          | Filtra por unidade solicitante. Para `ROLE_SOLICITANTE` é sempre sobrescrito pela unidade do próprio usuário (extraída do JWT); para `ROLE_REGULADOR` é opcional e livre |
| `status`     | enum    | ❌          | `AGUARDANDO`, `APROVADA`, etc.      |
| `destino`    | enum    | ❌          | `FILA_ESPERA` \| `FILA_REGULADA`    |
| `procedureId`| UUID    | ❌          | Filtrar por procedimento            |
| `page`       | integer | ❌          | Padrão `0`                          |
| `size`       | integer | ❌          | Padrão `20`                         |

**Response `200 OK`:** lista paginada de solicitações (mesmo schema do item acima).

---

### GET /api/v1/solicitacoes/{id}

Retorna o detalhe de uma solicitação.

> A resposta real **não inclui histórico de pareceres** — é um objeto plano (`SolicitacaoStatusResponseDTO`). Para ver as decisões emitidas, seria preciso um endpoint próprio de pareceres, que hoje não existe; versões anteriores deste documento mostravam um array `pareceres` aninhado que não corresponde à implementação.

**Auth:** `ROLE_SOLICITANTE` ou `ROLE_REGULADOR`

**Response `200 OK`:**
```json
{
  "id": "11223344-5566-7788-99aa-bbccddeeff00",
  "patientId": "f1e2d3c4-b5a6-7890-fedc-ba0987654321",
  "procedureId": "c0d1e2f3-a4b5-6789-cdef-012345678901",
  "cid": "I10",
  "justificativaClinica": "Paciente com hipertensão grave...",
  "profissionalSolicitante": "Dr. Ana Costa",
  "destino": "FILA_REGULADA",
  "riskColor": "AMARELO",
  "status": "APROVADA",
  "criadaEm": "2026-06-12T09:00:00Z",
  "updatedAt": "2026-06-12T11:30:00Z",
  "justificativaNegacao": null
}
```

**Erros:**

| Status | Motivo                       |
|--------|------------------------------|
| `404`  | Solicitação não encontrada   |

---

### POST /api/v1/solicitacoes/{id}/complementar

Complementa uma solicitação devolvida pelo regulador com informações faltantes.

**Auth:** `ROLE_SOLICITANTE`

**Request Body:**
```json
{
  "cid": "I11.9",
  "justificativaClinica": "Complemento: paciente com HAS estágio 3, sem resposta a 3 anti-hipertensivos.",
  "crmProfissional": "CRM/SP 12345"
}
```

| Campo                     | Tipo   | Obrigatório | Descrição                                        |
|---------------------------|--------|-------------|---------------------------------------------------|
| `cid`                     | string | ❌          | Novo código CID-10                                |
| `justificativaClinica`    | string | ❌          | Nova justificativa clínica                        |
| `crmProfissional`         | string | ❌          | Novo CRM do profissional                           |
| `profissionalSolicitante` | string | ❌          | Novo nome do médico solicitante                    |

> Apenas campos nullable podem ser atualizados. Após a complementação o status volta para `AGUARDANDO`.

**Response `200 OK`:** schema completo da solicitação atualizada.

**Erros:**

| Status | Motivo                                                         |
|--------|----------------------------------------------------------------|
| `404`  | Solicitação não encontrada                                     |
| `422`  | Status da solicitação não permite complementação — apenas `DEVOLVIDA` |

---

### DELETE /api/v1/solicitacoes/{id}

Cancela uma solicitação.

**Auth:** `ROLE_MEDICO` ou `ROLE_SOLICITANTE`

**Path Parameters:**

| Parâmetro | Tipo | Descrição            |
|-----------|------|----------------------|
| `id`      | UUID | ID da solicitação    |

**Response `204 No Content`**

**Erros:**

| Status | Motivo                                                   |
|--------|-----------------------------------------------------------|
| `404`  | Solicitação não encontrada                                 |
| `422`  | Status não permite cancelamento — apenas `AGUARDANDO`      |

---

### POST /api/v1/regulacao/{id}/avaliar

Emite um parecer sobre uma solicitação. Disponível apenas para o médico regulador.

**Auth:** `ROLE_REGULADOR`

**Request Body:**
```json
{
  "decisao": "AUTORIZAR",
  "riskColorDefinido": "AMARELO",
  "unidadeExecutanteId": "bb22cc33-dd44-ee55-ff66-007788990011",
  "justificativa": null
}
```

| Campo                 | Tipo   | Obrigatório | Descrição                                                         |
|-----------------------|--------|-------------|-------------------------------------------------------------------|
| `decisao`             | enum   | ✅          | `AUTORIZAR` \| `NEGAR` \| `DEVOLVER` \| `PENDENTE` \| `FILA_ESPERA` |
| `riskColorDefinido`   | enum   | condicional | Obrigatório apenas se `decisao` for `AUTORIZAR` ou `FILA_ESPERA` |
| `unidadeExecutanteId` | UUID   | condicional | Obrigatório apenas se `decisao` for `AUTORIZAR` ou `FILA_ESPERA` — unidade que executará o procedimento |
| `justificativa`       | string | condicional | Obrigatório se `decisao` for `NEGAR` ou `DEVOLVER`               |

**Efeitos por decisão:**

| Decisão       | Efeito                                                                |
|---------------|-----------------------------------------------------------------------|
| `AUTORIZAR`   | Publica `SOLICITATION_APPROVED` → queue-service cria entry em `FILA_REGULADA` |
| `FILA_ESPERA` | Publica `SOLICITATION_APPROVED` com destino `FILA_ESPERA`            |
| `NEGAR`       | Publica `SOLICITATION_DENIED` → notification-service notifica UBS    |
| `DEVOLVER`    | Publica `SOLICITATION_DEVOLVED` → UBS deve complementar e reenviar   |
| `PENDENTE`    | Aprovado mas sem vaga — aguarda cota ou decisão futura               |

**Response `200 OK`:**
```json
{
  "parecerId": "aabb1122-cc33-dd44-ee55-ff6677889900",
  "solicitacaoId": "11223344-5566-7788-99aa-bbccddeeff00",
  "reguladorId": "0f1e2d3c-4b5a-6789-0fed-cba098765432",
  "decisao": "AUTORIZAR",
  "justificativa": null,
  "emitidoEm": "2026-06-12T11:30:00Z",
  "novoStatus": "APROVADA"
}
```

> A resposta real (`AvaliarSolicitacaoResponseDTO`) é um objeto plano, sem aninhar um objeto `parecer` — e não inclui `riskColorDefinido`: o `Parecer` não tem coluna de cor (ver `erd.md`); a cor escolhida fica gravada em `solicitacoes.risco_solicitado`, consultável via `GET /api/v1/solicitacoes/{id}`.

**Erros:**

| Status | Motivo                                                              |
|--------|---------------------------------------------------------------------|
| `404`  | Solicitação não encontrada                                          |
| `422`  | Status não permite avaliação — `AUTORIZAR`/`FILA_ESPERA` exigem `AGUARDANDO` ou `PENDENTE`; `NEGAR`/`DEVOLVER`/`PENDENTE` exigem `AGUARDANDO` |
| `422`  | Justificativa obrigatória para decisão `NEGAR` ou `DEVOLVER`       |
| `422`  | `riskColorDefinido`/`unidadeExecutanteId` obrigatórios para decisão `AUTORIZAR` ou `FILA_ESPERA` |

---

### PATCH /api/v1/regulacao/solicitacoes/{id}/risco

Reclassifica a cor de risco de uma solicitação ainda não avaliada pelo regulador.

**Auth:** `ROLE_REGULADOR`

**Path Parameters:**

| Parâmetro | Tipo | Descrição            |
|-----------|------|----------------------|
| `id`      | UUID | ID da solicitação    |

**Request Body:**
```json
{
  "riskColor": "VERMELHO"
}
```

**Response `200 OK`:** schema completo da solicitação atualizada.

**Erros:**

| Status | Motivo                                                   |
|--------|-----------------------------------------------------------|
| `404`  | Solicitação não encontrada                                 |
| `422`  | Status não permite reclassificação                         |

---

### GET /api/v1/regulacao/pendentes

Lista solicitações com status `AGUARDANDO` aguardando avaliação do regulador, ordenadas por data de criação.

**Auth:** `ROLE_REGULADOR`

**Query Parameters:** `procedureId`, `page`, `size` (todos opcionais)

**Response `200 OK`:** lista paginada de solicitações com status `AGUARDANDO`.

---

### GET /api/v1/regulacao/pendentes-vaga

Lista solicitações aprovadas mas com status `PENDENTE` (sem vaga disponível no momento).

**Auth:** `ROLE_REGULADOR`

**Response `200 OK`:** lista paginada de solicitações com status `PENDENTE`.

---

## agendamento-service `:8084`

> Todos os endpoints abaixo exigem o header:
> ```
> Authorization: Bearer <token>
> ```
>
> O agendamento-service expõe **dois canais de API**:
> - **REST** (porta `8084`) — comandos que alteram estado (write operations)
> - **GraphQL** (porta `8084`, path `/graphql`) — consultas flexíveis (read operations)

---

### REST — Commands

#### PATCH /api/v1/appointments/{id}/confirmar

Confirma a presença do paciente no agendamento dentro do prazo de 72 horas.

**Auth:** `ROLE_PACIENTE`

**Response `200 OK`:**
```json
{
  "id": "cc44dd55-ee66-ff77-0011-223344556677",
  "status": "CONFIRMADO",
  "slotDateTime": "2026-07-10T08:30:00Z",
  "unitName": "UPA Norte",
  "unitAddress": "Rua das Flores, 100"
}
```

> `unitAddress` vem da coluna `endereco` de `HealthUnit` (adicionada para viabilizar este campo) — pode retornar `null` até que o cadastro de unidades seja atualizado com esse dado.

**Erros:**

| Status | Motivo                                                        |
|--------|---------------------------------------------------------------|
| `404`  | Agendamento não encontrado                                    |
| `422`  | Prazo de 72h expirado — status já foi alterado para `CANCELADO` |
| `422`  | Status não permite confirmação                                |

---

#### DELETE /api/v1/appointments/{id}

Cancela um agendamento e libera o slot alocado.

**Auth:** `ROLE_PACIENTE` ou `ROLE_MEDICO`

**Query Parameters:**

| Parâmetro | Tipo   | Obrigatório | Descrição          |
|-----------|--------|-------------|--------------------|
| `motivo`  | string | ❌          | Motivo do cancelamento |

**Response `204 No Content`**

**Erros:**

| Status | Motivo                                                         |
|--------|----------------------------------------------------------------|
| `404`  | Agendamento não encontrado                                     |
| `422`  | Status não permite cancelamento (`ATENDIDO`, `FALTOU`)        |

---

#### PATCH /api/v1/appointments/{id}/reagendar

Reagenda para outro slot disponível, liberando o slot atual.

**Auth:** `ROLE_MEDICO`

**Request Body:**
```json
{
  "newSlotId": "ff00aa11-bb22-cc33-dd44-ee5566778899"
}
```

**Response `200 OK`:** schema completo do appointment atualizado com o novo slot.

---

#### PATCH /api/v1/appointments/{id}/attend

Marca o agendamento como atendido (paciente compareceu).

**Auth:** `ROLE_EXECUTANTE`

**Response `200 OK`:**
```json
{
  "id": "cc44dd55-ee66-ff77-0011-223344556677",
  "status": "ATENDIDO",
  "attendedAt": "2026-07-10T08:35:00Z"
}
```

**Erros:**

| Status | Motivo                                            |
|--------|-----------------------------------------------------|
| `404`  | Agendamento não encontrado                          |
| `422`  | Agendamento deve estar `CONFIRMADO` para ser atendido |

---

#### POST /api/v1/appointments/{id}/falta

Registra a falta do paciente, libera o slot e publica o evento `PATIENT_NO_SHOW` que reinserirá o paciente na fila.

**Auth:** `ROLE_EXECUTANTE`

**Response `204 No Content`**

---

#### POST /api/v1/schedules

Cria uma grade semanal de atendimento para uma unidade executante e procedimento.

**Auth:** `ROLE_EXECUTANTE`

**Request Body:**
```json
{
  "unitId": "aa11bb22-cc33-dd44-ee55-ff6677889900",
  "providerId": "bb22cc33-dd44-ee55-ff66-007788990011",
  "procedureId": "c0d1e2f3-a4b5-6789-cdef-012345678901",
  "diaDaSemana": "SEGUNDA",
  "horarioInicio": "08:00",
  "horarioFim": "12:00",
  "slotDuracaoMinutos": 30,
  "capacidade": 2
}
```

**Response `201 Created`:**
```json
{
  "id": "uuid",
  "unitId": "aa11bb22-cc33-dd44-ee55-ff6677889900",
  "providerId": "bb22cc33-dd44-ee55-ff66-007788990011",
  "procedureId": "c0d1e2f3-a4b5-6789-cdef-012345678901",
  "diaDaSemana": "SEGUNDA",
  "horarioInicio": "08:00",
  "horarioFim": "12:00",
  "slotDuracaoMinutos": 30,
  "capacidade": 2,
  "ativo": true,
  "slotsGerados": 8
}
```

---

#### PUT /api/v1/schedules/{id}

Atualiza uma grade existente (horários, capacidade, profissional).

**Auth:** `ROLE_EXECUTANTE`

**Response `200 OK`:** schema completo da grade atualizada.

---

#### POST /api/v1/schedules/{id}/block

Bloqueia slots disponíveis de uma grade para uma data específica (feriado, manutenção, etc.).

**Auth:** `ROLE_EXECUTANTE`

**Request Body:**
```json
{
  "date": "2026-07-09",
  "motivo": "Feriado Nacional"
}
```

**Response `204 No Content`**

**Erros:**

| Status | Motivo                                                      |
|--------|---------------------------------------------------------------|
| `404`  | Grade não encontrada                                          |
| `404`  | Nenhum slot disponível encontrado para a data informada        |

---

#### POST /api/v1/schedules/{id}/unblock

Desbloqueia (volta para `DISPONIVEL`) todos os slots `INDISPONIVEL` da grade — não é filtrado por data.

**Auth:** `ROLE_EXECUTANTE`

**Response `204 No Content`**

**Erros:**

| Status | Motivo                                                 |
|--------|----------------------------------------------------------|
| `404`  | Grade não encontrada                                      |
| `404`  | Nenhum slot bloqueado (`INDISPONIVEL`) encontrado nessa grade |

---

### GraphQL — Queries

**Endpoint:** `POST /graphql`
**Playground:** `GET /graphiql`

---

#### Query: disponibilidade

Retorna slots disponíveis para um procedimento, com filtros opcionais de unidade e período.

```graphql
query {
  disponibilidade(
    procedureId: "c0d1e2f3-a4b5-6789-cdef-012345678901"
    unitId: "aa11bb22-cc33-dd44-ee55-ff6677889900"
    dataInicio: "2026-07-01"
    dataFim: "2026-07-07"
  ) {
    id
    dataHora
    capacity
    booked
    remainingCapacity
    schedule {
      unit {
        nome
        address
        cnes
      }
      provider {
        nome
        crm
        especialidade
      }
    }
  }
}
```

**Resposta:**
```json
{
  "data": {
    "disponibilidade": [
      {
        "id": "slot-uuid",
        "dataHora": "2026-07-01T08:30:00Z",
        "capacity": 2,
        "booked": 1,
        "remainingCapacity": 1,
        "schedule": {
          "unit": {
            "nome": "UPA Norte",
            "address": "Rua das Flores, 100",
            "cnes": "2077485"
          },
          "provider": {
            "nome": "Dr. Carlos Melo",
            "crm": "CRM/SP 98765",
            "especialidade": "Cardiologia"
          }
        }
      }
    ]
  }
}
```

---

#### Query: agendamentos

Retorna agendamentos com filtros flexíveis. PACIENTE vê apenas os próprios (pacienteId extraído do JWT).

```graphql
query {
  agendamentos(
    pacienteId: "f1e2d3c4-b5a6-7890-fedc-ba0987654321"
    status: CONFIRMADO
    dateFrom: "2026-07-01"
    dateTo: "2026-07-31"
  ) {
    id
    status
    expiresAt
    slot {
      dataHora
      schedule {
        unit { nome address }
        provider { nome especialidade }
      }
    }
    cancellationReason
  }
}
```

---

#### Query: agendamento (por ID)

```graphql
query {
  agendamento(id: "cc44dd55-ee66-ff77-0011-223344556677") {
    id
    status
    expiresAt
    slot {
      dataHora
      schedule {
        unit { nome address phone }
        provider { nome crm especialidade }
      }
    }
  }
}
```

---

#### Query: grade

Retorna a grade semanal de uma unidade para uma semana específica.

```graphql
query {
  grade(unitId: "aa11bb22-cc33-dd44-ee55-ff6677889900", week: "2026-07-07") {
    id
    dayOfWeek
    startTime
    endTime
    slotDurationMin
    capacity
    active
    provider { nome especialidade }
  }
}
```

---

#### Schema GraphQL completo

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
    dateFrom: String
    dateTo: String
  ): [Appointment!]!

  agendamento(id: ID!): Appointment

  grade(unitId: ID!, week: String!): [Schedule!]!
}

type Slot {
  id: ID!
  dataHora: String!
  capacity: Int!
  booked: Int!
  remainingCapacity: Int!
  status: SlotStatus!
  schedule: Schedule!
}

type Schedule {
  id: ID!
  dayOfWeek: String!
  startTime: String!
  endTime: String!
  slotDurationMin: Int!
  capacity: Int!
  active: Boolean!
  unit: HealthUnit!
  provider: Provider
}

type HealthUnit {
  id: ID!
  cnes: String!
  nome: String!
  address: String
  phone: String
}

type Provider {
  id: ID!
  nome: String!
  crm: String
  especialidade: String
}

type Appointment {
  id: ID!
  status: AppointmentStatus!
  expiresAt: String!
  cancellationReason: String
  createdAt: String!
  slot: Slot!
}

enum SlotStatus {
  DISPONIVEL
  RESERVADO
  OCUPADO
  INDISPONIVEL
}

enum AppointmentStatus {
  AGUARDANDO_CONFIRMACAO
  CONFIRMADO
  CANCELADO
  ATENDIDO
  FALTOU
}
```

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

| Valor               | Descrição                                                           |
|---------------------|---------------------------------------------------------------------|
| `AGUARDANDO`        | Paciente aguardando chamada (estado inicial)                        |
| `CHAMADO`           | Chamado pelo call-next; aguardando slot do agendamento-service      |
| `AGENDADO`          | Slot confirmado pelo agendamento-service                            |
| `ATENDIDO`          | Atendimento realizado                                               |
| `FALTOU`            | Paciente não compareceu após ser chamado                            |
| `CANCELADO`         | Entrada cancelada (pelo médico ou pelo paciente)                    |
| `DEVOLVIDO`         | Paciente devolvido à fila após não comparecimento (intermediário)   |

> `EQueueStatus` no código tem exatamente estes 7 valores. `AGUARDANDO_VAGA` (chamado mas sem slot disponível, ao consumir `appointment.no_slot`) **não está implementado** — não existe no enum. Além disso, hoje nenhum fluxo do domínio produz `ATENDIDO`, `FALTOU` ou `DEVOLVIDO`; `DEVOLVIDO` só é lido como precondição em `ReclassifyPriority`. Ver `requisitos-dominio-sus.md` §6.

### TipoFila

| Valor           | Descrição                                                              |
|-----------------|------------------------------------------------------------------------|
| `FILA_REGULADA` | Casos urgentes/prioritários — regulador define riskColor               |
| `FILA_ESPERA`   | Casos rotineiros — sempre AZUL, ordenação cronológica, controle de cota|

### StatusSolicitacao *(regulacao-service)*

| Valor       | Descrição                                        |
|-------------|--------------------------------------------------|
| `AGUARDANDO`| Criada, aguardando avaliação do regulador        |
| `APROVADA`  | Aprovada — entry criada no queue-service         |
| `NEGADA`    | Negada pelo regulador — UBS notificada           |
| `DEVOLVIDA` | Devolvida para complementação — UBS deve reenviar|
| `PENDENTE`  | Aprovada mas sem vaga — aguarda cota             |
| `CANCELADA` | Cancelada via `DELETE /api/v1/solicitacoes/{id}` |
| `AGENDADA`  | Agendamento confirmado pelo agendamento-service (evento `APPOINTMENT_CONFIRMED`) |
| `ATENDIDA`  | Atendimento realizado (evento `APPOINTMENT_ATTENDED`) |
| `FALTOU`    | Paciente faltou ao agendamento (evento `PATIENT_NO_SHOW`) |

### DecisaoRegulador *(regulacao-service)*

| Valor        | Efeito                                                      |
|--------------|-------------------------------------------------------------|
| `AUTORIZAR`  | Entry criada em FILA_REGULADA com riskColor do regulador    |
| `FILA_ESPERA`| Entry criada em FILA_ESPERA com riskColor AZUL              |
| `NEGAR`      | Solicitação negada — notificação à UBS                      |
| `DEVOLVER`   | Dados incompletos — UBS deve complementar                   |
| `PENDENTE`   | Aprovado sem vaga disponível                                |

### AppointmentStatus *(agendamento-service)*

| Valor                   | Descrição                                        |
|-------------------------|--------------------------------------------------|
| `AGUARDANDO_CONFIRMACAO`| Slot alocado, aguardando confirmação do paciente |
| `CONFIRMADO`            | Paciente confirmou presença                      |
| `CANCELADO`             | Agendamento cancelado                            |
| `ATENDIDO`              | Paciente compareceu e foi atendido               |
| `FALTOU`                | Paciente não compareceu                          |

### SlotStatus *(agendamento-service)*

| Valor          | Descrição                         |
|----------------|-----------------------------------|
| `DISPONIVEL`   | Slot disponível para alocação     |
| `RESERVADO`    | Reservado no banco (`chk_slots_status`) e no schema GraphQL — não é produzido pelo domínio Java hoje (`EStatusSlots` só tem `DISPONIVEL`/`OCUPADO`/`INDISPONIVEL`) |
| `OCUPADO`      | Capacidade esgotada               |
| `INDISPONIVEL` | Slot bloqueado (feriado etc.)     |

### NotificationType

| Valor                    | Contexto                                               |
|--------------------------|--------------------------------------------------------|
| `PATIENT_REGISTERED`     | Paciente adicionado à fila                             |
| `PATIENT_CALLED`         | Paciente chamado para atendimento                      |
| `PRIORITY_UPDATED`       | Cor de risco do paciente foi alterada                  |
| `PATIENT_CANCELLED`      | Entrada do paciente foi cancelada                      |
| `PATIENT_REINSTATED`     | Paciente reinserido na fila após falta ou expiração    |
| `APPOINTMENT_CONFIRMED`  | Agendamento confirmado com data, hora e local          |
| `APPOINTMENT_CANCELLED`  | Agendamento cancelado                                  |
| `APPOINTMENT_RESCHEDULED`| Agendamento reagendado para novo slot                  |
| `APPOINTMENT_NO_SLOT`    | Chamado mas sem slot disponível no momento             |
| `APPOINTMENT_EXPIRED`    | Prazo de 72h expirou sem confirmação                   |
| `SOLICITATION_DENIED`    | Solicitação negada — notifica UBS                      |
| `SOLICITATION_DEVOLVED`  | Solicitação devolvida para complementação              |

### UserRole

| Valor          | Prefixo Spring Security  | Permissões                                          |
|----------------|--------------------------|-----------------------------------------------------|
| `MEDICO`       | `ROLE_MEDICO`            | Gestão completa da fila e pacientes                 |
| `PACIENTE`     | `ROLE_PACIENTE`          | Consulta de posição, confirmação de agendamento     |
| `SOLICITANTE`  | `ROLE_SOLICITANTE`       | Criação e complementação de solicitações            |
| `REGULADOR`    | `ROLE_REGULADOR`         | Avaliação de solicitações e emissão de pareceres    |
| `EXECUTANTE`   | `ROLE_EXECUTANTE`        | Gestão de grades, slots e registro de faltas        |

---

## Respostas de Erro

Todos os serviços seguem o padrão **RFC 7807 — Problem Details for HTTP APIs**, implementado via `ProblemDetail` (Spring 6 nativo). O `Content-Type` das respostas de erro é `application/problem+json`.

### Campos obrigatórios (RFC 7807)

| Campo      | Tipo   | Descrição                                                                 |
|------------|--------|---------------------------------------------------------------------------|
| `type`     | URI    | Identifica o tipo do problema. URI única e estável por tipo de erro.      |
| `title`    | string | Descrição curta e legível do tipo do problema.                            |
| `detail`   | string | Mensagem específica desta ocorrência do erro.                             |
| `status`   | int    | Código HTTP da resposta.                                                  |
| `instance` | URI    | URI da request que originou o erro (ex: `/api/v1/patients`).             |

> Extension fields (campos extras fora da RFC) podem ser adicionados por tipo de erro — por exemplo, `violations` nos erros de validação.

---

**`400 Bad Request` — Validação de campos (`@Valid`):**
```json
{
  "type": "https://morbus.sus.gov.br/problems/validation-error",
  "title": "Requisição inválida",
  "detail": "Um ou mais campos são inválidos",
  "status": 400,
  "instance": "/api/v1/patients",
  "violations": [
    { "field": "cpf",             "message": "CPF inválido" },
    { "field": "dataNascimento",  "message": "não pode ser no futuro" }
  ]
}
```

**`400 Bad Request` — Corpo ausente ou malformado:**
```json
{
  "type": "https://morbus.sus.gov.br/problems/invalid-request-body",
  "title": "Requisição inválida",
  "detail": "Corpo da requisição ausente ou malformado",
  "status": 400,
  "instance": "/api/v1/queue"
}
```

**`401 Unauthorized` — Token ausente ou inválido:**
```json
{
  "type": "https://morbus.sus.gov.br/problems/invalid-credentials",
  "title": "Credenciais inválidas",
  "detail": "Token JWT ausente, inválido ou expirado",
  "status": 401,
  "instance": "/api/v1/queue"
}
```

**`403 Forbidden` — Role sem permissão:**
```json
{
  "type": "https://morbus.sus.gov.br/problems/access-denied",
  "title": "Acesso negado",
  "detail": "Perfil sem permissão para esta operação",
  "status": 403,
  "instance": "/api/v1/queue/call-next"
}
```

**`404 Not Found`:**
```json
{
  "type": "https://morbus.sus.gov.br/problems/queue-not-found",
  "title": "Recurso não encontrado",
  "detail": "QueueEntry com id 'e5f6a7b8-c9d0-1234-ef56-789012345678' não encontrada",
  "status": 404,
  "instance": "/api/v1/queue/e5f6a7b8-c9d0-1234-ef56-789012345678/position"
}
```

**`409 Conflict`:**
```json
{
  "type": "https://morbus.sus.gov.br/problems/patient-already-exists",
  "title": "Conflito de dados",
  "detail": "CPF '123.456.789-00' já cadastrado no sistema",
  "status": 409,
  "instance": "/api/v1/patients"
}
```

**`422 Unprocessable Entity` — Regra de negócio violada:**
```json
{
  "type": "https://morbus.sus.gov.br/problems/age-not-eligible",
  "title": "Regra de negócio violada",
  "detail": "Paciente não elegível: procedimento exige idade mínima de 18 anos",
  "status": 422,
  "instance": "/api/v1/queue"
}
```

**`500 Internal Server Error`:**
```json
{
  "type": "https://morbus.sus.gov.br/problems/internal-error",
  "title": "Erro interno",
  "detail": "Ocorreu um erro inesperado. Tente novamente.",
  "status": 500,
  "instance": "/api/v1/queue/call-next"
}
```

### URIs de tipo por serviço

| `type`                                                          | Status | Serviço(s)          |
|-----------------------------------------------------------------|--------|---------------------|
| `.../problems/validation-error`                                 | 400    | todos               |
| `.../problems/invalid-request-body`                             | 400    | todos               |
| `.../problems/invalid-credentials`                              | 401    | auth                |
| `.../problems/invalid-password`                                 | 422    | auth                |
| `.../problems/user-already-exists`                              | 409    | auth                |
| `.../problems/access-denied`                                    | 403    | todos               |
| `.../problems/patient-not-found`                                | 404    | queue               |
| `.../problems/procedure-not-found`                              | 404    | queue               |
| `.../problems/queue-not-found`                                  | 404    | queue               |
| `.../problems/queue-empty`                                      | 404    | queue               |
| `.../problems/queue-not-allowed`                                | 422    | queue               |
| `.../problems/patient-already-exists`                           | 409    | queue               |
| `.../problems/age-not-eligible`                                 | 422    | queue               |
| `.../problems/patient-not-eligible`                             | 422    | queue               |
| `.../problems/patient-already-registered`                       | 409    | queue               |
| `.../problems/patient-has-active-queue-entries`                 | 422    | queue               |
| `.../problems/active-queue-entries-exist`                       | 409    | queue               |
| `.../problems/procedure-already-assigned`                       | 409    | queue               |
| `.../problems/procedure-not-assigned`                           | 422    | queue               |
| `.../problems/patient-already-inactive`                         | 422    | queue               |
| `.../problems/patient-inactive`                                 | 422    | queue               |
| `.../problems/solicitation-not-found`                           | 404    | regulacao           |
| `.../problems/solicitation-already-exists`                      | 409    | regulacao           |
| `.../problems/regulacao-not-allowed`                            | 422    | regulacao           |
| `.../problems/quota-exceeded`                                   | 422    | regulacao / queue   |
| `.../problems/unit-not-found`                                   | 404    | regulacao           |
| `.../problems/unit-already-exists`                               | 409    | regulacao           |
| `.../problems/id-paciente-incorreto`                             | 403    | regulacao           |
| `.../problems/slot-not-found`                                   | 404    | agendamento         |
| `.../problems/slot-unavailable`                                 | 422    | agendamento         |
| `.../problems/appointment-not-found`                            | 404    | agendamento         |
| `.../problems/expired-confirmation`                             | 422    | agendamento         |
| `.../problems/appointment-already-exists`                       | 409    | agendamento         |
| `.../problems/schedule-already-exists`                          | 409    | agendamento         |
| `.../problems/schedule-not-found`                                | 404    | agendamento         |
| `.../problems/invalid-schedule-period`                           | 422    | agendamento         |
| `.../problems/appointment-not-allowed`                           | 422    | agendamento         |
| `.../problems/cancel-not-allowed`                                | 422    | agendamento         |
| `.../problems/notification-not-found`                            | 404    | notification        |
| `.../problems/internal-error`                                   | 500    | todos               |

> Base URL: `https://morbus.sus.gov.br`

---

*Documento atualizado em: julho/2026*
