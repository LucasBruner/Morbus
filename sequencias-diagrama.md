# Diagramas de Sequência — Sistema de Fila SUS
> Hackathon FIAP PosTech — Arquitetura e Desenvolvimento Java — Fase 5

---

## Índice

1. [Registro de Usuário](#1-registro-de-usuário)
2. [Login e Obtenção do JWT](#2-login-e-obtenção-do-jwt)
3. [Cadastro de Paciente](#3-cadastro-de-paciente)
4. [Inclusão na Fila](#4-inclusão-na-fila)
5. [Chamada do Próximo Paciente](#5-chamada-do-próximo-paciente)
6. [Reclassificação de Prioridade](#6-reclassificação-de-prioridade)
7. [Cancelamento de Entrada na Fila](#7-cancelamento-de-entrada-na-fila)
8. [Consulta de Posição pelo Paciente](#8-consulta-de-posição-pelo-paciente)
9. [Fluxo Completo — Visão Geral](#9-fluxo-completo--visão-geral)

---

## 1. Registro de Usuário

Criação de um novo usuário com role `MEDICO` ou `PACIENTE`.

```mermaid
sequenceDiagram
    actor Cliente
    participant AuthService as auth-service :8082
    participant DB as PostgreSQL

    Cliente->>AuthService: POST /auth/register<br/>{ username, email, password, role }

    AuthService->>AuthService: Valida campos obrigatórios<br/>e formato do e-mail

    AuthService->>DB: SELECT * FROM users WHERE username = ?<br/>OR email = ?
    DB-->>AuthService: resultado

    alt username ou email já existem
        AuthService-->>Cliente: 409 Conflict<br/>{ error: "CPF ou e-mail já cadastrado" }
    else dados válidos e únicos
        AuthService->>AuthService: bcrypt.hash(password, 10)
        AuthService->>DB: INSERT INTO users (id, username, email, password_hash, role, created_at)
        DB-->>AuthService: OK
        AuthService-->>Cliente: 201 Created<br/>{ id, username, email, role, createdAt }
    end
```

---

## 2. Login e Obtenção do JWT

Autenticação e emissão do token que será usado em todas as chamadas ao queue-service.

```mermaid
sequenceDiagram
    actor Cliente
    participant AuthService as auth-service :8082
    participant DB as PostgreSQL

    Cliente->>AuthService: POST /auth/login<br/>{ username, password }

    AuthService->>DB: SELECT * FROM users WHERE username = ?
    DB-->>AuthService: User { passwordHash, role }

    alt usuário não encontrado
        AuthService-->>Cliente: 401 Unauthorized<br/>{ error: "Credenciais inválidas" }
    else usuário encontrado
        AuthService->>AuthService: bcrypt.compare(password, passwordHash)

        alt senha incorreta
            AuthService-->>Cliente: 401 Unauthorized<br/>{ error: "Credenciais inválidas" }
        else senha correta
            AuthService->>AuthService: JWT.sign({ sub: username, role: ROLE_MEDICO },<br/>JWT_SECRET, { expiresIn: 24h })
            AuthService-->>Cliente: 200 OK<br/>{ token, type: "Bearer", expiresIn, role }
        end
    end
```

> O token gerado contém a **role** do usuário no payload e é **validado localmente** pelo queue-service a cada request, sem round-trip ao auth-service.

---

## 3. Cadastro de Paciente

Registro de um paciente no sistema pelo médico. O grupo legal é calculado automaticamente.

```mermaid
sequenceDiagram
    actor Medico as Médico
    participant QS as queue-service :8080
    participant Filter as JwtAuthFilter
    participant UC as UseCase<br/>RegisterPatient
    participant DB as PostgreSQL

    Medico->>QS: POST /api/v1/patients<br/>Authorization: Bearer <token><br/>{ cpf, nomeCompleto, dataNascimento, ... }

    QS->>Filter: intercepta request
    Filter->>Filter: Extrai e valida JWT<br/>Verifica role = ROLE_MEDICO
    alt token inválido ou expirado
        Filter-->>Medico: 401 Unauthorized
    else role insuficiente
        Filter-->>Medico: 403 Forbidden
    end
    Filter->>QS: SecurityContext populado

    QS->>DB: SELECT * FROM patients WHERE cpf = ?
    DB-->>QS: resultado

    alt CPF já cadastrado
        QS-->>Medico: 409 Conflict
    else CPF livre
        QS->>UC: execute(PatientCommand)
        UC->>UC: Calcula PriorityGroup:<br/>idade >= 60? → IDOSO<br/>gestante? → GESTANTE<br/>deficiente? → DEFICIENTE<br/>... → GERAL
        UC->>DB: INSERT INTO patients
        DB-->>UC: Patient { id, grupoLegal }
        UC-->>QS: PatientResponse
        QS-->>Medico: 201 Created<br/>{ id, cpf, nomeCompleto, grupoLegal, ... }
    end
```

---

## 4. Inclusão na Fila

Cadastro de um paciente em um procedimento. A entrada inicia com cor **AZUL** e publica evento para notificação.

```mermaid
sequenceDiagram
    actor Medico as Médico
    participant QS as queue-service :8080
    participant Filter as JwtAuthFilter
    participant UC as UseCase<br/>RegisterPatientInQueue
    participant Publisher as RabbitMQ<br/>Publisher
    participant MQ as RabbitMQ
    participant NS as notification-service :8081
    participant DB as PostgreSQL

    Medico->>QS: POST /api/v1/queue<br/>Authorization: Bearer <token><br/>{ patientId, procedureId }

    QS->>Filter: valida JWT → ROLE_MEDICO
    Filter->>QS: OK

    QS->>UC: execute(RegisterPatientCommand)

    UC->>DB: SELECT * FROM patients WHERE id = ?
    DB-->>UC: Patient

    alt paciente não encontrado
        UC-->>QS: NotFoundException
        QS-->>Medico: 404 Not Found
    end

    UC->>DB: SELECT * FROM procedures WHERE id = ?
    DB-->>UC: Procedure { idadeMinima, idadeMaxima }

    UC->>UC: Verifica elegibilidade:<br/>idadeMinima <= idadePaciente <= idadeMaxima

    alt fora da faixa etária
        UC-->>QS: IneligiblePatientException
        QS-->>Medico: 422 Unprocessable Entity<br/>{ error: "Paciente fora da faixa etária" }
    end

    UC->>DB: SELECT * FROM queue_entries<br/>WHERE patient_id = ? AND procedure_id = ?<br/>AND status = 'AGUARDANDO'
    DB-->>UC: resultado

    alt já possui entrada ativa
        UC-->>QS: DuplicateQueueEntryException
        QS-->>Medico: 409 Conflict
    end

    UC->>DB: INSERT INTO queue_entries<br/>{ riskColor: AZUL, status: AGUARDANDO }
    DB-->>UC: QueueEntry { id, position }

    UC->>Publisher: publishPatientRegistered(QueueEventDTO)
    Publisher->>MQ: Exchange: sus.queue.exchange<br/>Routing key: patient.registered<br/>Payload: { eventType, patientName, patientContact,<br/>procedureName, riskColor: AZUL, timestamp }

    UC-->>QS: QueueEntryResponse
    QS-->>Medico: 201 Created<br/>{ id, riskColor: AZUL, position, status: AGUARDANDO, ... }

    MQ-->>NS: Entrega mensagem na fila<br/>queue.patient.registered

    NS->>NS: QueueEventConsumer.consume(event)
    NS->>NS: NotificationService.process(event)<br/>Monta: "Você foi cadastrado na fila para<br/>[procedimento]. Classificação: AZUL."
    NS->>DB: INSERT INTO notifications
    NS->>NS: EmailService.send(contato, assunto, corpo)<br/>[EMAIL SIMULADO] Para: joao@email.com | ...
```

---

## 5. Chamada do Próximo Paciente

O médico chama o próximo da fila. O sistema aplica o algoritmo de prioridade e dispara a notificação.

```mermaid
sequenceDiagram
    actor Medico as Médico
    participant QS as queue-service :8080
    participant Filter as JwtAuthFilter
    participant UC as UseCase<br/>CallNextPatient
    participant Publisher as RabbitMQ<br/>Publisher
    participant MQ as RabbitMQ
    participant NS as notification-service :8081
    participant DB as PostgreSQL

    Medico->>QS: POST /api/v1/queue/call-next<br/>Authorization: Bearer <token>

    QS->>Filter: valida JWT → ROLE_MEDICO
    Filter->>QS: OK

    QS->>UC: execute()

    UC->>DB: SELECT * FROM queue_entries<br/>WHERE status = 'AGUARDANDO'<br/>ORDER BY risk_color ASC,<br/>priority_group ASC,<br/>registered_at ASC<br/>LIMIT 1
    DB-->>UC: QueueEntry (maior prioridade) | vazio

    alt fila vazia
        UC-->>QS: QueueEmptyException
        QS-->>Medico: 404 Not Found<br/>{ error: "Nenhum paciente aguardando na fila" }
    end

    Note over UC,DB: Paciente encontrado: Maria, VERMELHO, IDOSO

    UC->>DB: UPDATE queue_entries<br/>SET status = 'AGENDADO', updated_at = NOW()<br/>WHERE id = ?
    DB-->>UC: OK

    UC->>Publisher: publishPatientCalled(QueueEventDTO)
    Publisher->>MQ: Exchange: sus.queue.exchange<br/>Routing key: patient.called<br/>Payload: { eventType: PATIENT_CALLED,<br/>patientName, patientContact,<br/>procedureName, riskColor, timestamp }

    UC-->>QS: CalledPatientResponse
    QS-->>Medico: 200 OK<br/>{ id, patient, procedure,<br/>riskColor, status: AGENDADO, calledAt }

    MQ-->>NS: Entrega mensagem na fila<br/>queue.patient.called

    NS->>NS: QueueEventConsumer.consume(event)
    NS->>NS: NotificationService.process(event)<br/>Monta: "É a sua vez! Compareça ao<br/>guichê para [procedimento]."
    NS->>DB: INSERT INTO notifications
    NS->>NS: EmailService.send(...)<br/>[EMAIL SIMULADO] Para: maria@email.com | ...
```

---

## 6. Reclassificação de Prioridade

O médico altera a cor de risco de um paciente já na fila (ex: estado clínico piorou).

```mermaid
sequenceDiagram
    actor Medico as Médico
    participant QS as queue-service :8080
    participant Filter as JwtAuthFilter
    participant UC as UseCase<br/>ReclassifyPriority
    participant Publisher as RabbitMQ<br/>Publisher
    participant MQ as RabbitMQ
    participant NS as notification-service :8081
    participant DB as PostgreSQL

    Medico->>QS: PATCH /api/v1/queue/{id}/priority<br/>Authorization: Bearer <token><br/>{ riskColor: "AMARELO" }

    QS->>Filter: valida JWT → ROLE_MEDICO
    Filter->>QS: OK

    QS->>UC: execute(queueEntryId, AMARELO)

    UC->>DB: SELECT * FROM queue_entries WHERE id = ?
    DB-->>UC: QueueEntry { riskColor: AZUL, status: AGUARDANDO }

    alt entrada não encontrada
        UC-->>QS: NotFoundException
        QS-->>Medico: 404 Not Found
    end

    alt status não permite reclassificação (ex: ATENDIDO)
        UC-->>QS: InvalidStatusException
        QS-->>Medico: 422 Unprocessable Entity
    end

    UC->>DB: UPDATE queue_entries<br/>SET risk_color = 'AMARELO', updated_at = NOW()<br/>WHERE id = ?
    DB-->>UC: OK

    UC->>DB: SELECT COUNT(*) FROM queue_entries<br/>WHERE status = 'AGUARDANDO'<br/>AND (risk_color < 'AMARELO'<br/>OR (risk_color = 'AMARELO' AND registered_at < ?))
    DB-->>UC: newPosition = 4

    UC->>Publisher: publishPriorityUpdated(QueueEventDTO)
    Publisher->>MQ: Exchange: sus.queue.exchange<br/>Routing key: priority.updated<br/>Payload: { eventType: PRIORITY_UPDATED,<br/>riskColor: AMARELO, ... }

    UC-->>QS: ReclassifyResponse
    QS-->>Medico: 200 OK<br/>{ id, riskColor: AMARELO,<br/>newPosition: 4, updatedAt }

    MQ-->>NS: Entrega mensagem na fila<br/>queue.priority.updated

    NS->>NS: QueueEventConsumer.consume(event)
    NS->>NS: NotificationService.process(event)<br/>Monta: "Sua prioridade na fila foi<br/>atualizada para AMARELO."
    NS->>DB: INSERT INTO notifications
    NS->>NS: EmailService.send(...)
```

---

## 7. Cancelamento de Entrada na Fila

Remoção de um paciente da fila pelo médico, com notificação automática.

```mermaid
sequenceDiagram
    actor Medico as Médico
    participant QS as queue-service :8080
    participant Filter as JwtAuthFilter
    participant UC as UseCase<br/>CancelQueueEntry
    participant Publisher as RabbitMQ<br/>Publisher
    participant MQ as RabbitMQ
    participant NS as notification-service :8081
    participant DB as PostgreSQL

    Medico->>QS: DELETE /api/v1/queue/{id}?motivo=...<br/>Authorization: Bearer <token>

    QS->>Filter: valida JWT → ROLE_MEDICO
    Filter->>QS: OK

    QS->>UC: execute(queueEntryId, motivo)

    UC->>DB: SELECT * FROM queue_entries WHERE id = ?
    DB-->>UC: QueueEntry { status: AGUARDANDO }

    alt entrada não encontrada
        UC-->>QS: NotFoundException
        QS-->>Medico: 404 Not Found
    end

    alt status = ATENDIDO (não pode cancelar)
        UC-->>QS: InvalidStatusException
        QS-->>Medico: 422 Unprocessable Entity
    end

    UC->>DB: UPDATE queue_entries<br/>SET status = 'CANCELADO', updated_at = NOW()<br/>WHERE id = ?
    DB-->>UC: OK

    UC->>Publisher: publishPatientCancelled(QueueEventDTO)
    Publisher->>MQ: Exchange: sus.queue.exchange<br/>Routing key: patient.cancelled

    UC-->>QS: void
    QS-->>Medico: 204 No Content

    MQ-->>NS: Entrega mensagem na fila<br/>queue.patient.cancelled

    NS->>NS: NotificationService.process(event)<br/>Monta: "Seu agendamento para<br/>[procedimento] foi cancelado."
    NS->>DB: INSERT INTO notifications
    NS->>NS: EmailService.send(...)
```

---

## 8. Consulta de Posição pelo Paciente

O paciente consulta sua posição atual na fila sem precisar ter role de médico.

```mermaid
sequenceDiagram
    actor Paciente
    participant QS as queue-service :8080
    participant Filter as JwtAuthFilter
    participant UC as UseCase<br/>GetQueuePosition
    participant DB as PostgreSQL

    Paciente->>QS: GET /api/v1/queue/{id}/position<br/>Authorization: Bearer <token>

    QS->>Filter: intercepta request
    Filter->>Filter: Extrai e valida JWT<br/>Verifica role = ROLE_PACIENTE ou ROLE_MEDICO

    alt token inválido
        Filter-->>Paciente: 401 Unauthorized
    end

    Filter->>QS: SecurityContext populado

    QS->>UC: execute(queueEntryId)

    UC->>DB: SELECT * FROM queue_entries WHERE id = ?
    DB-->>UC: QueueEntry { riskColor, priorityGroup, status, registeredAt }

    alt entrada não encontrada
        UC-->>QS: NotFoundException
        QS-->>Paciente: 404 Not Found
    end

    UC->>DB: SELECT COUNT(*) FROM queue_entries<br/>WHERE status = 'AGUARDANDO'<br/>AND (risk_color < entry.riskColor<br/>OR (risk_color = entry.riskColor AND priority_group < entry.priorityGroup)<br/>OR (risk_color = entry.riskColor AND priority_group = entry.priorityGroup<br/>     AND registered_at < entry.registeredAt))
    DB-->>UC: totalAhead = 2

    UC->>UC: position = totalAhead + 1<br/>estimatedWaitMonths = meses(riskColor)

    UC-->>QS: QueuePositionResponse
    QS-->>Paciente: 200 OK<br/>{ id, position: 3, totalAhead: 2,<br/>riskColor, priorityGroup, status,<br/>estimatedWaitMonths: 12, registeredAt }
```

---

## 9. Fluxo Completo — Visão Geral

Visão macro de uma jornada completa: do login até o atendimento do paciente.

```mermaid
sequenceDiagram
    actor Medico as Médico
    actor Paciente
    participant Auth as auth-service
    participant QS as queue-service
    participant MQ as RabbitMQ
    participant NS as notification-service

    Note over Medico,NS: FASE 1 — Autenticação

    Medico->>Auth: POST /auth/login
    Auth-->>Medico: JWT (ROLE_MEDICO)

    Paciente->>Auth: POST /auth/login
    Auth-->>Paciente: JWT (ROLE_PACIENTE)

    Note over Medico,NS: FASE 2 — Cadastro

    Medico->>QS: POST /api/v1/patients (JWT Médico)
    QS-->>Medico: 201 Paciente cadastrado

    Medico->>QS: POST /api/v1/queue (JWT Médico)<br/>riskColor = AZUL (padrão)
    QS-->>Medico: 201 Entrada na fila criada (posição 5)
    QS--)MQ: Evento: PATIENT_REGISTERED
    MQ--)NS: Consome evento
    NS-->>Paciente: [EMAIL] "Você foi cadastrado na fila"

    Note over Medico,NS: FASE 3 — Acompanhamento

    Paciente->>QS: GET /api/v1/queue/{id}/position (JWT Paciente)
    QS-->>Paciente: posição: 5, espera estimada: 12 meses

    Note over Medico,NS: FASE 4 — Reclassificação (piora clínica)

    Medico->>QS: PATCH /api/v1/queue/{id}/priority (JWT Médico)<br/>{ riskColor: "AMARELO" }
    QS-->>Medico: 200 nova posição: 2
    QS--)MQ: Evento: PRIORITY_UPDATED
    MQ--)NS: Consome evento
    NS-->>Paciente: [EMAIL] "Sua prioridade foi atualizada para AMARELO"

    Paciente->>QS: GET /api/v1/queue/{id}/position (JWT Paciente)
    QS-->>Paciente: posição: 2, espera estimada: 3 meses

    Note over Medico,NS: FASE 5 — Chamada

    Medico->>QS: POST /api/v1/queue/call-next (JWT Médico)
    QS-->>Medico: 200 Paciente chamado: Maria Oliveira
    QS--)MQ: Evento: PATIENT_CALLED
    MQ--)NS: Consome evento
    NS-->>Paciente: [EMAIL] "É a sua vez! Compareça ao guichê"
```

---

## Legenda dos Participantes

| Participante            | Descrição                                                            |
|-------------------------|----------------------------------------------------------------------|
| **Médico**              | Profissional de saúde com `ROLE_MEDICO` — gerencia a fila           |
| **Paciente**            | Usuário com `ROLE_PACIENTE` — consulta sua posição                  |
| **auth-service**        | Serviço de autenticação, emite e valida credenciais                 |
| **queue-service**       | Núcleo do sistema — aplica regras de negócio e publica eventos      |
| **JwtAuthFilter**       | Filtro Spring Security que intercepta e valida o Bearer token       |
| **UseCase**             | Camada Application do queue-service (Clean Architecture)            |
| **RabbitMQ Publisher**  | Porta de saída na camada Infrastructure — publica eventos no broker |
| **RabbitMQ**            | Message broker — desacopla queue-service do notification-service    |
| **notification-service**| Consome eventos e envia notificações ao paciente                    |
| **PostgreSQL**          | Banco de dados compartilhado pelos serviços                         |

---

*Documento gerado em: maio/2026*
