# Implementação: Lógica de Priorização de Fila SUS

## Resumo

> ⚠️ **A ordenação real da fila (`GET /api/v1/queue`, `GET /api/v1/queue/{id}/position`, `POST /api/v1/queue/call-next`) é feita por `ORDER BY` em JPQL, em `QueueEntryJpaRepository`, não por `PriorityCalculator.compare()`.** `PriorityCalculator.compare()` existe no domínio mas não é chamado por nenhum usecase de listagem/posição/chamada — hoje só `getPriorityGroup()`/`isPriorityGroup()` são usados em produção, e apenas em `RegisterPatient`/`UpdatePatient` (cadastro/atualização de paciente), não na ordenação da fila. As duas implementações divergem entre si (ver seção 0 abaixo).

O algoritmo de ordenação da fila ambulatorial do SUS, com suporte a dois tipos de fila — `FILA_REGULADA` e `FILA_ESPERA` — que seguem regras de ordenação distintas, é implementado de duas formas que **não são idênticas**: a query JPQL usada pela API (fonte de verdade em runtime) e a classe `PriorityCalculator` (domínio puro, mas não conectada à ordenação real).

---

## 0. Onde a ordenação realmente acontece

`QueueEntryJpaRepository` (`findByPriority`, `findAllOrderedByPriority`, `findByProcedureIdAndFilters`) define a ordenação com uma query JPQL:

```jpql
ORDER BY
    CASE q.tipoFila WHEN FILA_REGULADA THEN 0 ELSE 1 END ASC,
    q.riskColor ASC,
    q.patient.grupoLegal ASC,
    q.registeredAt ASC
```

Isso cobre os 4 critérios documentados (tipoFila → cor → grupo → chegada), mas com uma diferença importante em relação ao que as seções abaixo descrevem para `PriorityCalculator`:

- **`q.patient.grupoLegal` é o valor armazenado em `patients.grupo_legal`**, não um recálculo dinâmico. Esse valor só é recalculado (via `PriorityCalculator.getPriorityGroup()`) quando o paciente é **cadastrado** (`RegisterPatient`) ou **atualizado** (`UpdatePatient`) — nunca no momento da consulta/ordenação da fila. Um paciente que completa 60 anos **enquanto já está na fila** não é promovido a `IDOSO` "na próxima ordenação" como versões anteriores deste documento afirmavam — ele só é promovido se/quando seu cadastro for atualizado novamente.
- `PriorityCalculator.compare()`, por outro lado, **não implementa o Critério 1** (tipoFila) — ele compara apenas riskColor → priorityGroup → registeredAt, e o `groupScore` usado ali vem de `getPriorityGroup(patient)` (dinâmico), diferente da query JPQL acima. Como esse método não é chamado por nenhum usecase, essa diferença hoje não afeta o comportamento da API, mas o texto do método é enganoso se lido como "o algoritmo que roda em produção".

---

## Tipos de Fila (EDestino)

| Tipo            | Descrição                                                        |
|-----------------|------------------------------------------------------------------|
| `FILA_REGULADA` | Casos urgentes ou prioritários. Regulador define a cor de risco. Algoritmo completo de 3 critérios em cascata. |
| `FILA_ESPERA`   | Casos rotineiros. Cor de risco sempre `AZUL`. Ordenação cronológica pura. Sujeita a cotas por UBS. |

> **Regra global:** entradas em `FILA_REGULADA` sempre precedem entradas em `FILA_ESPERA` na ordenação da fila, independentemente da cor de risco ou tempo de espera.

---

## Algoritmo de Ordenação

A comparação entre duas entradas segue **4 critérios em cascata**:

### Critério 1 — Tipo de fila (FILA_REGULADA antes de FILA_ESPERA)

```
FILA_REGULADA (ordinal 0) < FILA_ESPERA (ordinal 1)
```

Toda entrada regulada ocupa posição antes de qualquer entrada de espera. Esse critério é avaliado antes de qualquer comparação de cor ou grupo.

### Critério 2 — Cor de risco (somente FILA_REGULADA)

Aplicado apenas quando as duas entradas são do mesmo tipo `FILA_REGULADA`. Em `FILA_ESPERA` este critério é ignorado (todos são `AZUL`).

| Cor        | Prioridade numérica | Tempo máximo de espera |
|------------|---------------------|------------------------|
| `VERMELHO` | 1 (maior urgência)  | 1 mês                  |
| `AMARELO`  | 2                   | 3 meses                |
| `VERDE`    | 3                   | 6 meses                |
| `AZUL`     | 4 (menor urgência)  | 1 ano                  |

### Critério 3 — Grupo de prioridade (somente FILA_REGULADA)

Aplicado apenas dentro de `FILA_REGULADA` quando duas entradas têm a mesma cor de risco.

| Grupo        | Prioridade | Critério de elegibilidade                                     |
|--------------|------------|---------------------------------------------------------------|
| `IDOSO`      | 1 (maior)  | Idade ≥ 60 anos (calculado automaticamente por `getPriorityGroup`) |
| `GESTANTE`   | 2          | Campo `gestante = true`                                       |
| `DEFICIENTE` | 3          | Campo `deficiente = true`                                     |
| `LACTANTE`   | 4          | Campo `lactante = true`                                       |
| `OBESO`      | 5          | Campo `obeso = true`                                          |
| `GERAL`      | 6 (menor)  | Padrão                                                        |

> Pacientes com 60 anos ou mais recebem automaticamente o grupo `IDOSO`, sobrescrevendo o valor de `grupoLegal` cadastrado — regra implementada em `getPriorityGroup(Patient)`.

### Critério 4 — Timestamp de chegada (desempate final)

Aplicado em qualquer situação onde os critérios anteriores empatam. Quem entrou primeiro na fila tem prioridade. É o **único critério** usado dentro de `FILA_ESPERA`.

---

## Implementação — PriorityCalculator.java

**Localização:** `queue-service/src/main/java/br/com/morbus/queueservice/domain/service/`

### compare(QueueEntry a, QueueEntry b)

> Código real (`domain/service/PriorityCalculator.java`). **Não implementa o Critério 1** (tipoFila) — compara só riskColor → priorityGroup → registeredAt. A precedência FILA_REGULADA-antes-de-FILA_ESPERA só existe na query JPQL usada pela API (seção 0), não aqui. Este método também não é chamado por nenhum usecase hoje.

```java
public static int compare(QueueEntry a, QueueEntry b) {
    if (a == null && b == null) return 0;
    if (a == null) return 1;
    if (b == null) return -1;

    // Cor de risco
    int riskComparison = Integer.compare(
        a.getRiskColor().getNumericPriority(),
        b.getRiskColor().getNumericPriority()
    );
    if (riskComparison != 0) return riskComparison;

    // Grupo de prioridade (dinâmico — recalcula idade a cada chamada)
    int priorityGroupComparison = Integer.compare(
        getPriorityGroup(a.getPatient()).getNumericPriority(),
        getPriorityGroup(b.getPatient()).getNumericPriority()
    );
    if (priorityGroupComparison != 0) return priorityGroupComparison;

    // Desempate: timestamp de chegada
    return a.getRegisteredAt().compareTo(b.getRegisteredAt());
}
```

### getPriorityGroup(Patient patient)

Retorna o grupo de prioridade efetivo do paciente. Idosos (≥ 60 anos) recebem `IDOSO` automaticamente.

> **Chamado apenas em `RegisterPatient` e `UpdatePatient`** — o resultado é gravado em `patients.grupo_legal` no momento do cadastro/atualização. A ordenação da fila (seção 0) lê esse valor já armazenado; não recalcula a idade a cada consulta. Ou seja: um paciente cadastrado aos 58 anos com `grupoLegal = GERAL` só passa a ser tratado como `IDOSO` na fila depois que seu cadastro for atualizado (`PATCH /api/v1/patients/{id}`) após completar 60 — não automaticamente "na próxima ordenação" enquanto ele aguarda.

```java
public static EPriorityGroup getPriorityGroup(Patient patient) {
    if (patient == null) return EPriorityGroup.GERAL;
    if (isPriorityGroup(patient)) return EPriorityGroup.IDOSO;
    return patient.getGrupoLegal() != null ? patient.getGrupoLegal() : EPriorityGroup.GERAL;
}
```

### isPriorityGroup(Patient patient)

```java
public static boolean isPriorityGroup(Patient patient) {
    if (patient == null || patient.getDataNascimento() == null) return false;
    return Period.between(patient.getDataNascimento(), LocalDate.now()).getYears() >= 60;
}
```

---

## Como Usar

### Ordenar a fila completa

> Este é o uso *previsto* de `compare()` — mas nenhum usecase real chama `fila.sort(PriorityCalculator::compare)` hoje. A ordenação que a API de fato retorna vem da query JPQL da seção 0, e essa query **inclui** o critério de tipoFila que `compare()` não tem.

```java
List<QueueEntry> fila = repository.findAll();
fila.sort(PriorityCalculator::compare);
// Não reflete o comportamento real da API — ver seção 0.
```

### Verificar tipo de fila antes de reclassificar

```java
// ReclassifyPriority use case — bloqueio para FILA_ESPERA
if (queueEntry.getTipoFila() == EDestino.FILA_ESPERA) {
    throw new QueueNotAllowedException(
        "Entradas em FILA_ESPERA não podem ter a cor de risco alterada (sempre AZUL)"
    );
}
```

### Verificar grupo efetivo do paciente

```java
EPriorityGroup grupo = PriorityCalculator.getPriorityGroup(patient);
// Retorna IDOSO se 60+, independentemente do campo grupoLegal
```

---

## Regras de Validação por Tipo de Fila

### Inserção em FILA_ESPERA

| Regra             | Descrição                                                                                      |
|-------------------|------------------------------------------------------------------------------------------------|
| Cor de risco      | Deve ser obrigatoriamente `AZUL`. Qualquer outra cor é rejeitada com `422`.                   |
| Cota da UBS       | `CheckAndEnforceQuota` verifica `currentCount < maxPerPeriod`. Lança `QuotaExceededException` se esgotada. |
| Reclassificação   | Proibida. Apenas `FILA_REGULADA` pode ter a cor alterada via `PATCH /priority`.               |

### Inserção em FILA_REGULADA

| Regra             | Descrição                                                                   |
|-------------------|-----------------------------------------------------------------------------|
| Cor de risco      | Inserida como `AZUL`. Somente o regulador pode reclassificar.               |
| Cota              | Sem controle de cota.                                                       |
| Reclassificação   | Permitida nos status `AGUARDANDO` e `DEVOLVIDO`.                            |

---

## Exemplo de Ordenação

Dada a fila abaixo (antes da ordenação):

| Paciente         | tipoFila        | riskColor | grupoLegal | registeredAt     |
|------------------|-----------------|-----------|------------|------------------|
| Carlos (72 anos) | FILA_ESPERA     | AZUL      | IDOSO      | 2026-01-01 08:00 |
| Ana              | FILA_REGULADA   | VERMELHO  | GESTANTE   | 2026-01-05 10:00 |
| Pedro (65 anos)  | FILA_REGULADA   | AMARELO   | IDOSO      | 2026-01-03 09:00 |
| Joana            | FILA_REGULADA   | AMARELO   | GERAL      | 2026-01-02 07:00 |
| Marcos           | FILA_ESPERA     | AZUL      | GERAL      | 2026-01-04 11:00 |

Após a ordenação real da API (query JPQL da seção 0 — não `PriorityCalculator.compare()`, que não teria aplicado o critério de tipoFila):

| Pos. | Paciente         | Motivo                                                      |
|------|------------------|-------------------------------------------------------------|
| 1    | Ana              | FILA_REGULADA + VERMELHO (prioridade máxima)                |
| 2    | Pedro (65 anos)  | FILA_REGULADA + AMARELO + IDOSO (≥ 60 anos)                 |
| 3    | Joana            | FILA_REGULADA + AMARELO + GERAL + chegou antes que... (não há outro AMARELO+GERAL) |
| 4    | Carlos (72 anos) | FILA_ESPERA + chegou antes (01/01) — grupo ignorado         |
| 5    | Marcos           | FILA_ESPERA + chegou depois (04/01)                         |

> Carlos tem 72 anos mas está em `FILA_ESPERA`, onde somente o timestamp importa. Ele não ocupa posição 1 apenas por ser idoso — toda `FILA_REGULADA` precede `FILA_ESPERA`.
>
> Nesta tabela, `grupoLegal = IDOSO` para Pedro e Carlos assume que seus cadastros de paciente já refletem essa condição (setada em `RegisterPatient`/`UpdatePatient`). Se um paciente completa 60 anos **depois** de cadastrado, ele só passa a contar como `IDOSO` na fila após uma atualização de cadastro — a ordenação em si não recalcula a idade.

---

## Testes Unitários — PriorityCalculatorTest

| Cenário                                                     | Resultado esperado                       |
|-------------------------------------------------------------|------------------------------------------|
| FILA_REGULADA vs FILA_ESPERA (mesma cor, mesmo grupo)       | FILA_REGULADA sempre antes               |
| FILA_REGULADA: VERMELHO vs AMARELO                          | VERMELHO antes                           |
| FILA_REGULADA: mesma cor, IDOSO vs GERAL                    | IDOSO antes                              |
| FILA_REGULADA: mesma cor, mesmo grupo, timestamps distintos | Quem chegou antes                        |
| FILA_ESPERA: ignora cor e grupo, ordena por timestamp       | Cronológico puro                         |
| Paciente com 60+ anos e grupoLegal = GERAL                  | `getPriorityGroup` retorna `IDOSO`       |
| Paciente com 59 anos                                        | `getPriorityGroup` retorna `grupoLegal`  |
| `patient = null` em `getPriorityGroup`                      | Retorna `GERAL` sem NPE                  |
| `dataNascimento = null` em `isPriorityGroup`                | Retorna `false` sem NPE                  |

---

*Documento atualizado em: julho/2026 — revisado contra `PriorityCalculator.java` e `QueueEntryJpaRepository.java` reais.*
