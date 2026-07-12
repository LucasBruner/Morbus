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

Criação de um novo usuário. O campo `role` aceita um dos cinco valores: `MEDICO`, `PACIENTE`, `SOLICITANTE`, `REGULADOR` ou `EXECUTANTE`.

```mermaid
sequenceDiagram
    actor Cliente
    participant AuthService as auth-service :8082
    participant DB as PostgreSQL

    Cliente->>AuthService: POST /auth/register<br/>{ username, email, password,<br/>role: MEDICO|PACIENTE|SOLICITANTE|REGULADOR|EXECUTANTE }

    alt role com valor fora do enum UserRole
        AuthService-->>Cliente: 400 Bad Request (application/problem+json)<br/>{ type: ".../invalid-request-body",<br/>title: "Requisição inválida",<br/>detail: "Valor inválido para 'role'. Valores aceitos: MEDICO, PACIENTE, SOLICITANTE, REGULADOR, EXECUTANTE",<br/>status: 400 }
    else senha não atende à política de complexidade
        AuthService->>AuthService: validatePassword(password)<br/>regex: ao menos 1 minúscula, 1 maiúscula,<br/>1 caractere não-alfanumérico e 9+ caracteres
        AuthService-->>Cliente: 422 Unprocessable Entity (application/problem+json)<br/>{ type: ".../invalid-password",<br/>title: "Senha inválida",<br/>status: 422 }
    else demais validações (campos obrigatórios, formato do e-mail)
        AuthService->>DB: SELECT * FROM users WHERE username = ?<br/>OR email = ?
        DB-->>AuthService: resultado

        alt username ou email já existem
            AuthService-->>Cliente: 409 Conflict (application/problem+json)<br/>{ type: ".../user-already-exists",<br/>title: "Conflito de dados",<br/>detail: "Username ou e-mail já cadastrado",<br/>status: 409 }
        else dados válidos e únicos
            AuthService->>AuthService: bcrypt.hash(password, 10)
            AuthService->>DB: INSERT INTO users (id, username, email, password_hash, role, created_at)
            DB-->>AuthService: OK
            AuthService-->>Cliente: 201 Created<br/>{ id, username, email, role, createdAt }
        end
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
        AuthService-->>Cliente: 401 Unauthorized (application/problem+json)<br/>{ type: ".../invalid-credentials",<br/>title: "Credenciais inválidas",<br/>detail: "Usuário ou senha incorretos!",<br/>status: 401 }
    else usuário encontrado
        AuthService->>AuthService: bcrypt.compare(password, passwordHash)

        alt senha incorreta
            AuthService-->>Cliente: 401 Unauthorized (application/problem+json)<br/>{ type: ".../invalid-credentials",<br/>title: "Credenciais inválidas",<br/>detail: "Usuário ou senha incorretos!",<br/>status: 401 }
        else senha correta
            AuthService->>AuthService: JWT.sign({ sub: username, role: <ROLE_DO_USUARIO> },<br/>JWT_SECRET, { expiresIn: 24h })
            AuthService-->>Cliente: 200 OK<br/>{ token, type: "Bearer", expiresIn, role }
        end
    end
```

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

    UC->>Publisher: publishPatientRegistered(QueueEntry)
    Publisher->>MQ: Exchange: sus.queue.exchange<br/>Routing key: patient.registered<br/>Payload: { eventType, patientName, patientContact,<br/>procedureName, riskColor: AZUL, timestamp }

    UC-->>QS: QueueEntryResponse
    QS-->>Medico: 201 Created<br/>{ id, riskColor: AZUL, position, status: AGUARDANDO, ... }

    MQ-->>NS: Entrega mensagem na fila<br/>notification.queue.events

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

    UC->>DB: UPDATE queue_entries<br/>SET status = 'CHAMADO', updated_at = NOW()<br/>WHERE id = ?
    DB-->>UC: OK

    UC->>Publisher: publishPatientCalled(QueueEntry)
    Publisher->>MQ: Exchange: sus.queue.exchange<br/>Routing key: patient.called<br/>Payload: { eventType: PATIENT_CALLED,<br/>patientName, patientContact,<br/>procedureName, riskColor, timestamp }

    UC-->>QS: CalledPatientResponse
    QS-->>Medico: 200 OK<br/>{ id, patient, procedure,<br/>riskColor, status: CHAMADO, calledAt }

    MQ-->>NS: Entrega mensagem na fila<br/>notification.queue.events

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

    UC->>Publisher: publishPriorityUpdated(QueueEntry)
    Publisher->>MQ: Exchange: sus.queue.exchange<br/>Routing key: priority.updated<br/>Payload: { eventType: PRIORITY_UPDATED,<br/>riskColor: AMARELO, ... }

    UC-->>QS: ReclassifyResponse
    QS-->>Medico: 200 OK<br/>{ id, riskColor: AMARELO,<br/>newPosition: 4, updatedAt }

    MQ-->>NS: Entrega mensagem na fila<br/>notification.queue.events

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

    UC->>Publisher: publishPatientCancelled(QueueEntry)
    Publisher->>MQ: Exchange: sus.queue.exchange<br/>Routing key: patient.cancelled

    UC-->>QS: void
    QS-->>Medico: 204 No Content

    MQ-->>NS: Entrega mensagem na fila<br/>notification.queue.events

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

---

## 10. Solicitação Aprovada → Fila → Agendamento (Caminho Feliz)

Fluxo completo de ponta a ponta: UBS cria solicitação, regulador aprova, paciente entra na fila, é chamado e recebe slot confirmado.

```mermaid
sequenceDiagram
    actor Solicitante as Solicitante (UBS)
    actor Regulador
    actor Paciente
    participant RS as regulacao-service :8083
    participant MQ as RabbitMQ
    participant QS as queue-service :8080
    participant AS as agendamento-service :8084
    participant NS as notification-service :8081

    Solicitante->>RS: POST /api/v1/solicitacoes<br/>{ patientId, procedureId, cid, justificativa, destino: FILA_REGULADA }
    RS->>RS: Valida dados, verifica duplicidade
    RS-->>Solicitante: 201 Created { id, status: AGUARDANDO }

    Note over Regulador,RS: Avaliação pelo Regulador

    Regulador->>RS: GET /api/v1/regulacao/pendentes
    RS-->>Regulador: [ solicitação 11223344... ]

    Regulador->>RS: POST /api/v1/regulacao/11223344.../avaliar<br/>{ decisao: AUTORIZAR, riskColorDefinido: AMARELO }
    RS->>RS: Cria Parecer, atualiza status → APROVADA
    RS--)MQ: SOLICITATION_APPROVED<br/>{ solicitacaoId, patientId, procedureId, riskColor: AMARELO, tipoFila: FILA_REGULADA }
    RS-->>Regulador: 200 OK { novoStatus: APROVADA }

    Note over MQ,QS: queue-service consome o evento

    MQ--)QS: Consome SOLICITATION_APPROVED
    QS->>QS: AddToQueue → cria QueueEntry<br/>status: AGUARDANDO, tipoFila: FILA_REGULADA, riskColor: AMARELO
    QS--)MQ: PATIENT_REGISTERED
    MQ--)NS: Consome PATIENT_REGISTERED
    NS-->>Paciente: [EMAIL] "Você foi incluído na fila para [procedimento]. Classificação: AMARELO"

    Note over Regulador,AS: Chamada do próximo

    Regulador->>QS: POST /api/v1/queue/call-next
    QS->>QS: CallNextPatient → status: CHAMADO
    QS--)MQ: PATIENT_CALLED { queueEntryId, patientId, procedureId, preferredUnitId }
    QS-->>Regulador: 200 OK { status: CHAMADO }

    Note over MQ,AS: agendamento-service aloca slot

    MQ--)AS: Consome PATIENT_CALLED
    AS->>AS: AlocarSlot — busca slot disponível para procedimento/unidade/faixa etária
    AS->>AS: Cria Appointment { status: AGUARDANDO_CONFIRMACAO, expiresAt: +72h }
    AS--)MQ: APPOINTMENT_CONFIRMED { queueEntryId, slotDateTime, unitName, unitAddress }

    MQ--)QS: Consome APPOINTMENT_CONFIRMED
    QS->>QS: Atualiza QueueEntry: CHAMADO → AGENDADO

    MQ--)NS: Consome APPOINTMENT_CONFIRMED
    NS-->>Paciente: [EMAIL] "Consulta agendada para 10/07/2026 às 08:30 na UPA Norte, Rua das Flores 100"

    Note over Paciente,AS: Confirmação de presença

    Paciente->>AS: PATCH /api/v1/appointments/{id}/confirmar
    AS->>AS: status: AGUARDANDO_CONFIRMACAO → CONFIRMADO
    AS-->>Paciente: 200 OK { status: CONFIRMADO, slotDateTime, unitName }
```

---

## 11. Solicitação Negada pelo Regulador

```mermaid
sequenceDiagram
    actor Solicitante as Solicitante (UBS)
    actor Regulador
    participant RS as regulacao-service :8083
    participant MQ as RabbitMQ
    participant NS as notification-service :8081

    Solicitante->>RS: POST /api/v1/solicitacoes { ... }
    RS-->>Solicitante: 201 Created { status: AGUARDANDO }

    Regulador->>RS: POST /api/v1/regulacao/{id}/avaliar<br/>{ decisao: NEGAR, riskColorDefinido: AZUL, justificativa: "Procedimento não indicado para o CID informado" }
    RS->>RS: Cria Parecer, atualiza status → NEGADA
    RS--)MQ: SOLICITATION_DENIED { solicitacaoId, justificativa }
    RS-->>Regulador: 200 OK { novoStatus: NEGADA }

    MQ--)NS: Consome SOLICITATION_DENIED
    NS-->>Solicitante: [EMAIL] "Solicitação negada. Motivo: Procedimento não indicado para o CID informado"
```

---

## 12. Agendamento Expirado — Reinserção na Fila

Quando o paciente não confirma presença em 72 horas, o job de expiração cancela o agendamento e o queue-service reinserirá o paciente na fila.

```mermaid
sequenceDiagram
    participant Job as @Scheduled (a cada 15 min)
    participant AS as agendamento-service :8084
    participant MQ as RabbitMQ
    participant QS as queue-service :8080
    participant NS as notification-service :8081
    actor Paciente

    Note over Job,AS: Job de verificação periódica (AppointmentExpirationJob, a cada 15 min — cron "0 */15 * * * *")

    Job->>AS: verifyExpiredAppointments()
    AS->>AS: SELECT appointments WHERE status = AGUARDANDO_CONFIRMACAO AND expires_at < NOW()
    AS->>AS: Para cada expirado: status → CANCELADO
    AS--)MQ: APPOINTMENT_EXPIRED { queueEntryId, patientId, procedureName }

    MQ--)QS: Consome APPOINTMENT_EXPIRED
    QS->>QS: QueueEntry: AGENDADO → AGUARDANDO (reinserido no fim da fila)
    QS--)MQ: PATIENT_REINSTATED { queueEntryId, patientId }

    MQ--)NS: Consome APPOINTMENT_EXPIRED
    NS-->>Paciente: [EMAIL] "Seu agendamento para [procedimento] foi cancelado por não confirmação em 72h. Você voltou à fila."

    MQ--)NS: Consome PATIENT_REINSTATED
    NS-->>Paciente: [EMAIL] "Você foi reinserido na fila para [procedimento]. Aguarde nova chamada."
```

---

## 13. Paciente Faltou — Reinserção na Fila

```mermaid
sequenceDiagram
    actor Executante
    participant AS as agendamento-service :8084
    participant MQ as RabbitMQ
    participant QS as queue-service :8080
    participant NS as notification-service :8081
    actor Paciente

    Executante->>AS: POST /api/v1/appointments/{id}/falta
    AS->>AS: status: CONFIRMADO → FALTOU
    AS->>AS: Libera slot (booked--)
    AS--)MQ: PATIENT_NO_SHOW { queueEntryId, patientId }
    AS-->>Executante: 204 No Content

    MQ--)QS: Consome PATIENT_NO_SHOW
    QS->>QS: QueueEntry: AGENDADO → AGUARDANDO (reinserido no fim da fila)
    QS--)MQ: PATIENT_REINSTATED { queueEntryId }

    MQ--)NS: Consome PATIENT_REINSTATED
    NS-->>Paciente: [EMAIL] "Você foi reinserido na fila para [procedimento] após não comparecimento."
```

---

## Legenda dos Participantes

| Participante              | Descrição                                                                  |
|---------------------------|----------------------------------------------------------------------------|
| **Médico**                | Profissional com `ROLE_MEDICO` — gerencia a fila                          |
| **Paciente**              | Usuário com `ROLE_PACIENTE` — consulta posição e confirma presença        |
| **Solicitante (UBS)**     | Operador com `ROLE_SOLICITANTE` — cria solicitações na UBS                |
| **Regulador**             | Médico regulador com `ROLE_REGULADOR` — avalia e emite pareceres          |
| **Executante**            | Responsável da unidade com `ROLE_EXECUTANTE` — registra faltas e grades   |
| **auth-service**          | Serviço de autenticação, emite JWT                                        |
| **regulacao-service**     | Upstream — gerencia solicitações e pareceres (Hexagonal Architecture)     |
| **queue-service**         | Núcleo do sistema — fila priorizada (Clean Architecture)                  |
| **agendamento-service**   | Downstream — gerencia slots e agendamentos (CQRS + GraphQL)               |
| **notification-service**  | Consome todos os eventos e notifica pacientes e UBS (Event-driven)        |
| **RabbitMQ**              | Message broker — desacopla todos os serviços                              |
| **PostgreSQL**            | Banco de dados — cada serviço tem seu próprio schema                      |
| **@Scheduled**            | Job Spring periódico no agendamento-service para expiração de 72h         |

---

*Documento gerado em: maio/2026*
