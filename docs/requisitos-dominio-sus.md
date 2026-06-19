# Requisitos e Domínio do SUS — Fila Ambulatorial
> Hackathon FIAP PosTech — Arquitetura e Desenvolvimento Java — Fase 5

---

## 1. Contexto — SISREG e a Fila Ambulatorial

O **SISREG** (Sistema de Regulação) é o sistema do Ministério da Saúde utilizado para gerenciar a oferta e a demanda de serviços ambulatoriais e hospitalares do SUS. No fluxo ambulatorial, uma **Unidade Básica de Saúde (UBS)** solicita um procedimento para um paciente; um **médico regulador** avalia a solicitação e, se aprovada, o paciente entra numa fila priorizada aguardando um slot de atendimento na **unidade executante**.

O **Morbus** digitaliza e estende esse fluxo, implementando as regras de prioridade clínica e legal exigidas pelo SUS e pela legislação vigente.

---

## 2. Protocolo de Cores de Risco (Classificação Clínica)

A classificação de risco determina a urgência clínica do paciente e é o critério primário de priorização dentro da `FILA_REGULADA`.

| Cor        | Ordinal | Urgência    | Tempo máximo de espera | Critério de classificação                             |
|------------|---------|-------------|------------------------|-------------------------------------------------------|
| `VERMELHO` | 0       | Urgente     | ≤ 1 mês               | Condição clínica grave, deterioração iminente         |
| `AMARELO`  | 1       | Prioritário | ≤ 3 meses             | Condição moderada com risco de agravamento            |
| `VERDE`    | 2       | Eletivo     | ≤ 6 meses             | Condição estável, procedimento indicado               |
| `AZUL`     | 3       | Rotina      | ≤ 12 meses            | Entrada padrão — toda entrada nova começa em AZUL     |

> **Regra de entrada:** toda solicitação é criada com cor `AZUL`. O regulador pode elevar a cor no ato da avaliação. O médico pode reclassificar uma entrada existente em `FILA_REGULADA` via `PATCH /api/v1/queue/{id}/priority`.
>
> **Restrição de FILA_ESPERA:** entradas em `FILA_ESPERA` são sempre `AZUL` e não admitem reclassificação de cor.

---

## 3. Grupos de Prioridade Legal

A prioridade legal é o segundo critério de ordenação dentro da `FILA_REGULADA`, aplicada somente quando dois pacientes têm a mesma cor de risco. Baseia-se na legislação federal:

| Grupo        | Ordinal | Base Legal                                          | Critério de elegibilidade                              |
|--------------|---------|-----------------------------------------------------|--------------------------------------------------------|
| `IDOSO`      | 1       | Lei 10.741/2003 — Estatuto do Idoso                 | Idade ≥ 60 anos (detectado automaticamente)            |
| `GESTANTE`   | 2       | Lei 9.263/1996 — Planejamento Familiar              | Campo `grupoLegal = GESTANTE` no cadastro              |
| `DEFICIENTE` | 3       | Lei 10.048/2000 — Prioridade a Portadores de Deficiência | Campo `grupoLegal = DEFICIENTE`                  |
| `LACTANTE`   | 4       | Lei 10.048/2000 — Prioridade a Lactantes            | Campo `grupoLegal = LACTANTE`                          |
| `OBESO`      | 5       | Lei 11.347/2006 — Atenção ao Paciente Diabético e Obeso | Campo `grupoLegal = OBESO`                        |
| `GERAL`      | 6       | —                                                   | Nenhum critério especial — grupo padrão                |

### 3.1 Detecção automática de IDOSO

O `PriorityCalculator.getPriorityGroup(Patient patient)` verifica a idade do paciente em tempo real:

```java
// Paciente com 60+ anos → IDOSO (sobrescreve grupoLegal cadastrado)
if (Period.between(patient.getDataNascimento(), LocalDate.now()).getYears() >= 60) {
    return EPriorityGroup.IDOSO;
}
return patient.getGrupoLegal();
```

> Isso garante que um paciente que completou 60 anos após o cadastro seja automaticamente promovido ao grupo `IDOSO` na próxima ordenação, sem necessidade de recadastro.

---

## 4. Algoritmo de Ordenação da Fila

Implementado em `PriorityCalculator` (domínio puro, sem dependências de framework). Quatro critérios em cascata:

### Critério 1 — Tipo de Fila

`FILA_REGULADA` sempre precede `FILA_ESPERA`, independentemente de cor ou tempo de espera.

```
FILA_REGULADA (0) < FILA_ESPERA (1)
```

**Justificativa:** pacientes regulados foram avaliados por um médico regulador e têm indicação clínica formal. Pacientes em `FILA_ESPERA` aguardam em lista de rotina, sujeita a controle de cotas por UBS.

### Critério 2 — Cor de Risco (somente FILA_REGULADA)

```
VERMELHO (0) < AMARELO (1) < VERDE (2) < AZUL (3)
```

### Critério 3 — Grupo de Prioridade Legal (somente FILA_REGULADA, mesma cor)

```
IDOSO (1) < GESTANTE (2) < DEFICIENTE (3) < LACTANTE (4) < OBESO (5) < GERAL (6)
```

### Critério 4 — Timestamp de Chegada (desempate universal)

`registeredAt ASC` — quem entrou primeiro tem prioridade. É o único critério dentro de `FILA_ESPERA`.

### Exemplo de ordenação completo

| # | Paciente         | Tipo           | Cor       | Grupo Legal | Chegada        | Motivo                                      |
|---|------------------|----------------|-----------|-------------|----------------|---------------------------------------------|
| 1 | Ana              | FILA_REGULADA  | VERMELHO  | GERAL       | 05/01 10h      | Cor mais urgente                            |
| 2 | Pedro (65 anos)  | FILA_REGULADA  | AMARELO   | IDOSO*      | 03/01 09h      | IDOSO automático por ter ≥ 60 anos          |
| 3 | Joana (gestante) | FILA_REGULADA  | AMARELO   | GESTANTE    | 03/01 10h      | Mesma cor, GESTANTE > GERAL                 |
| 4 | Carlos           | FILA_REGULADA  | AMARELO   | GERAL       | 02/01 07h      | Mesma cor, chegou antes de Marcos           |
| 5 | Marcos           | FILA_REGULADA  | AMARELO   | GERAL       | 04/01 11h      | Mesma cor, chegou depois                    |
| 6 | Rita (72 anos)   | FILA_ESPERA    | AZUL      | IDOSO       | 01/01 08h      | FILA_ESPERA — ordenação cronológica pura    |
| 7 | Bruno            | FILA_ESPERA    | AZUL      | GERAL       | 04/01 11h      | FILA_ESPERA — chegou depois de Rita         |

> *Pedro tem `grupoLegal = GERAL`, mas `getPriorityGroup()` retorna `IDOSO` porque tem 65 anos.

---

## 5. Fluxo SISREG — Ciclo de Vida Completo

```
[UBS / SOLICITANTE]
      │ POST /api/v1/solicitacoes
      ▼
[regulacao-service] ────── status: AGUARDANDO ──────▶ REGULADOR avalia
      │                                                      │
      │  AUTORIZAR → publica SOLICITATION_APPROVED            │
      │  NEGAR     → publica SOLICITATION_DENIED              │
      │  DEVOLVER  → UBS complementa → volta para AGUARDANDO  │
      │  PENDENTE  → aprovado sem vaga (aguarda cota)         │
      ▼
[queue-service] ── cria QueueEntry (status: AGUARDANDO, riskColor = regulador)
      │
      │ Médico: POST /api/v1/queue/call-next
      ▼
QueueEntry: AGUARDANDO → AGENDADO
      │ publica PATIENT_CALLED
      ▼
[agendamento-service] ── aloca slot ── cria Appointment (AGUARDANDO_CONFIRMACAO)
      │ publica APPOINTMENT_CONFIRMED
      ▼
[notification-service] ── e-mail: "Consulta agendada para DD/MM/YYYY às HH:MM na UPA X"
      │
      │ Paciente: PATCH /api/v1/appointments/{id}/confirmar (prazo 72h)
      ▼
Appointment: CONFIRMADO
      │ No dia do atendimento
      │ Executante: POST /api/v1/appointments/{id}/falta  ─▶  FALTOU → reinserção na fila
      ▼
Appointment: ATENDIDO → QueueEntry: ATENDIDO
```

### 5.1 Tipos de Fila

| Tipo            | Origem                              | Cor permitida       | Algoritmo de ordenação          | Controle de cota |
|-----------------|-------------------------------------|---------------------|---------------------------------|------------------|
| `FILA_REGULADA` | Aprovação pelo regulador            | VERMELHO/AMARELO/VERDE/AZUL | tipoFila → cor → grupo → chegada | Não          |
| `FILA_ESPERA`   | Aprovação com destino FILA_ESPERA   | Sempre AZUL         | Somente chegada (FIFO)          | Sim — por UBS    |

### 5.2 Controle de Cotas (FILA_ESPERA)

A tabela `unit_procedure_quotas` controla quantas inserções em `FILA_ESPERA` cada UBS pode fazer por procedimento dentro de um período. Ao atingir `max_per_period`, novas solicitações dessa combinação UBS + procedimento recebem status `PENDENTE` até abertura de nova vaga.

---

## 6. Ciclo de Vida dos Status

### QueueEntry (queue-service)

| Transição                       | Gatilho                                          |
|---------------------------------|--------------------------------------------------|
| AGUARDANDO → AGENDADO           | `CallNextPatient` (POST /call-next)              |
| AGUARDANDO → CANCELADO          | `CancelQueueEntry` (DELETE /queue/{id})          |
| AGENDADO → ATENDIDO             | Evento `appointment.attended` do agendamento     |
| AGENDADO → FALTOU               | Evento `appointment.no_show` do agendamento      |
| FALTOU → DEVOLVIDO → AGUARDANDO | Evento `patient.reinstated` (reinserção)         |
| AGUARDANDO_VAGA → AGUARDANDO    | Evento `appointment.expired` (sem slot disponível)|

### Solicitação (regulacao-service)

| Transição                         | Gatilho                                |
|-----------------------------------|----------------------------------------|
| AGUARDANDO → APROVADA             | Regulador emite parecer AUTORIZAR      |
| AGUARDANDO → NEGADA               | Regulador emite parecer NEGAR          |
| AGUARDANDO → DEVOLVIDA            | Regulador emite parecer DEVOLVER       |
| AGUARDANDO → PENDENTE             | Regulador aprova mas sem vaga na cota  |
| DEVOLVIDA → AGUARDANDO            | UBS complementa a solicitação          |
| PENDENTE → APROVADA               | Cota disponível — geração de QueueEntry|
| APROVADA → AGENDADA               | Evento `appointment.created`           |
| AGENDADA → ATENDIDA               | Evento `appointment.attended`          |
| AGENDADA → FALTOU                 | Evento `appointment.no_show`           |

---

## 7. Regras de Negócio Implementadas

| Regra | Onde | Descrição |
|-------|------|-----------|
| Reclassificação de cor | `ReclassifyPriority` | Só permitida em status `AGUARDANDO` ou `DEVOLVIDO`, apenas em `FILA_REGULADA` |
| Cancelamento | `CancelQueueEntry` | Só permitido em status `AGUARDANDO` ou `AGENDADO` |
| FILA_ESPERA sempre AZUL | `AddToQueue` | Ao criar entrada em FILA_ESPERA, cor é forçada para AZUL |
| Unicidade na fila | `AddToQueue` | Paciente não pode ter duas entradas ativas (`AGUARDANDO`/`DEVOLVIDO`) para o mesmo procedimento |
| Elegibilidade etária | `AddToQueue` | Paciente deve estar dentro da faixa `idadeMinima`-`idadeMaxima` do procedimento |
| IDOSO automático | `PriorityCalculator` | Idade ≥ 60 anos na data da ordenação → grupo IDOSO, independente do cadastro |
| Cota por UBS | `CheckAndEnforceQuota` | Somente para FILA_ESPERA; bloqueia inserção se `current_count >= max_per_period` |

---

## 8. Referências Legais

| Lei / Norma                     | Ementa                                                               | Grupo beneficiado   |
|---------------------------------|----------------------------------------------------------------------|---------------------|
| Lei 10.741/2003 (Estatuto do Idoso) | Dispõe sobre o Estatuto do Idoso — art. 3º e 15º: atendimento preferencial para pessoas com 60+ anos | `IDOSO` |
| Lei 10.048/2000                 | Dá prioridade de atendimento a pessoas com deficiência, gestantes, lactantes, idosos e obesos | `DEFICIENTE`, `GESTANTE`, `LACTANTE`, `OBESO` |
| Lei 9.263/1996                  | Regula o planejamento familiar — inclui pré-natal e proteção às gestantes | `GESTANTE` |
| Lei 11.347/2006                 | Dispõe sobre a distribuição gratuita de medicamentos e materiais para portadores de diabetes e obesos | `OBESO` |
| Portaria GM/MS 1.559/2008       | Institui a Política Nacional de Regulação do SUS — base do fluxo SISREG | geral |
| Portaria GM/MS 2.309/2001       | Institui o SISREG — sistema de regulação de serviços de saúde | geral |

---

*Documento criado em: junho/2026*
