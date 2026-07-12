# Implementação: Lógica de Priorização de Fila SUS

## Resumo

> ⚠️ **A ordenação real da fila (`GET /api/v1/queue`, `GET /api/v1/queue/{id}/position`, `POST /api/v1/queue/call-next`) é feita por `ORDER BY` em JPQL, em `QueueEntryJpaRepository`, não por `PriorityCalculator.compare()`.** `PriorityCalculator.compare()` existe no domínio mas não é chamado por nenhum usecase de listagem/posição/chamada. O grupo de prioridade dentro dessa query JPQL, no entanto, **é recalculado dinamicamente por idade a cada consulta** (não é um snapshot estático) — ver seção 0 abaixo.

O algoritmo de ordenação da fila ambulatorial do SUS, com suporte a dois tipos de fila — `FILA_REGULADA` e `FILA_ESPERA` — que seguem regras de ordenação distintas, é implementado de duas formas que **não são idênticas**: a query JPQL usada pela API (fonte de verdade em runtime) e a classe `PriorityCalculator` (domínio puro, mas não conectada à ordenação real).

---

## 0. Onde a ordenação realmente acontece

`QueueEntryJpaRepository` (`findByPriority`, `findAllOrderedByPriority`, `findByProcedureIdAndFilters`) define a ordenação com uma query JPQL:

```jpql
ORDER BY
    CASE q.tipoFila WHEN FILA_REGULADA THEN 0 ELSE 1 END ASC,
    q.riskColor ASC,
    <EFFECTIVE_PRIORITY_GROUP> ASC,
    q.registeredAt ASC
```

Isso cobre os 4 critérios documentados (tipoFila → cor → grupo → chegada). `<EFFECTIVE_PRIORITY_GROUP>` é uma constante JPQL (`QueueEntryJpaRepository.EFFECTIVE_PRIORITY_GROUP`) que **recalcula a idade do paciente a cada consulta** — não lê `patients.grupo_legal` diretamente para decidir `IDOSO`:

```jpql
CASE WHEN (
    YEAR(CURRENT_DATE) - YEAR(q.patient.dataNascimento) -
    CASE WHEN (MONTH(CURRENT_DATE) < MONTH(q.patient.dataNascimento))
           OR (MONTH(CURRENT_DATE) = MONTH(q.patient.dataNascimento) AND DAY(CURRENT_DATE) < DAY(q.patient.dataNascimento))
         THEN 1 ELSE 0 END
) >= 60 THEN 0 ELSE
    CASE q.patient.grupoLegal
        WHEN EPriorityGroup.IDOSO THEN 0 WHEN EPriorityGroup.GESTANTE THEN 1
        WHEN EPriorityGroup.DEFICIENTE THEN 2 WHEN EPriorityGroup.LACTANTE THEN 3
        WHEN EPriorityGroup.OBESO THEN 4 ELSE 5
    END
END
```

- Se a idade calculada em SQL (`CURRENT_DATE` vs `patients.data_nascimento`) já é ≥ 60, a entrada conta como `IDOSO` (ordinal 0) **mesmo que `patients.grupo_legal` ainda esteja gravado como outro grupo** — não depende de um `PATCH /api/v1/patients/{id}` prévio. `patients.grupo_legal` só é usado como fallback para os grupos que não são detectáveis por idade (`GESTANTE`/`DEFICIENTE`/`LACTANTE`/`OBESO`).
- `PriorityCalculator.compare()`, por outro lado, **não implementa o Critério 1** (tipoFila) — ele compara apenas riskColor → priorityGroup → registeredAt. Como esse método não é chamado por nenhum usecase, essa diferença hoje não afeta o comportamento da API, mas o texto do método é enganoso se lido como "o algoritmo que roda em produção".
- Coberto por testes de integração em `QueueFlowIntegrationTest.OrdenacaoDinamicaPorIdade` (H2) — um paciente registrado com menos de 60 anos, cujo cadastro nunca é atualizado, é corretamente promovido a `IDOSO` na ordenação assim que sua idade real cruza os 60 anos.

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

| Cor        | Prioridade numérica |
|------------|---------------------|
| `VERMELHO` | 1 (maior urgência)  |
| `AMARELO`  | 2                   |
| `VERDE`    | 3                   |
| `AZUL`     | 4 (menor urgência)  |

> ⚠️ Uma versão anterior desta tabela incluía uma coluna de "tempo máximo de espera" por cor (1 mês/3 meses/6 meses/1 ano). Isso é uma referência clínica do protocolo de risco, mas **não existe implementação alguma** no código (`queue-service` nem `regulacao-service`) que leia, calcule ou aplique SLA de tempo máximo de espera por `riskColor` — nenhum job, listener ou validação usa esse dado hoje.

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

> Chamado em `RegisterPatient`, `UpdatePatient` e `RegisterPatientInQueue` para gravar um *snapshot* em `patients.grupo_legal`. A ordenação da fila (seção 0), porém, **não depende apenas desse snapshot** para o grupo `IDOSO`: a query JPQL recalcula a idade a cada consulta via `EFFECTIVE_PRIORITY_GROUP`, espelhando esta mesma lógica em SQL. Um paciente cadastrado aos 58 anos com `grupoLegal = GERAL` é automaticamente tratado como `IDOSO` na fila assim que sua idade real atinge 60 — sem precisar de um novo `PATCH /api/v1/patients/{id}`.

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
        "Entradas em FILA_ESPERA não podem ter a cor de risco reclassificada"
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
| Cota da UBS       | `CheckAndEnforceQuota` verifica `used >= quota.getMaxPerDay()` (campo real da entidade `UnitProcedureQuota` é `maxPerDay`, não `maxPerPeriod`). Lança `QuotaExceededException` se esgotada. |
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
> `IDOSO` para Pedro e Carlos vale independentemente de quando seus cadastros foram feitos: a ordenação recalcula a idade a cada consulta, então mesmo que `patients.grupo_legal` estivesse desatualizado (ex: gravado como `GERAL` antes de completarem 60 anos), a query já os trataria como `IDOSO` hoje.

---

## Testes Unitários — PriorityCalculatorTest

> `PriorityCalculator.compare()` não implementa o Critério 1 (tipoFila) — ver seção 0. Por isso não existem (nem poderiam existir, com esse resultado) testes que comparem `FILA_REGULADA` vs `FILA_ESPERA` dentro de `compare()`; esse critério só é coberto pela query JPQL, testada em `QueueFlowIntegrationTest`.

| Cenário                                                            | Resultado esperado                       |
|---------------------------------------------------------------------|-------------------------------------------|
| Risco Vermelho vs Azul                                             | Vermelho antes                           |
| Ordem completa de cores: Vermelho > Amarelo > Verde > Azul         | Ordem respeitada                         |
| Mesma cor, Idoso vs Geral                                          | Idoso antes                              |
| Idoso vermelho vs Geral vermelho                                   | Idoso antes                              |
| Timestamps distintos, mesmo tudo o mais                            | Quem chegou antes                        |
| Múltiplos pacientes no mesmo timestamp                              | Comparação estável (sem exceção)         |
| Gestante vs Idoso (ambos prioritários)                              | Conforme prioridade numérica do grupo    |
| Mesmo tudo (cor, grupo, timestamp)                                  | `compare()` retorna 0 (empate)           |
| `isPriorityGroup`: paciente com 60 anos exatos                     | Retorna `true`                           |
| `isPriorityGroup`: paciente com 60+ anos                           | Retorna `true`                           |
| `isPriorityGroup`: paciente com menos de 60 anos                   | Retorna `false`                          |
| `isPriorityGroup`: paciente nulo                                   | Retorna `false`                          |
| `getPriorityGroup`: paciente com 60+ anos                          | Retorna `IDOSO`                          |
| `getPriorityGroup`: paciente com menos de 60 anos (testado com 30) | Retorna `grupoLegal`                     |
| `getPriorityGroup`: paciente sem grupo legal definido              | Retorna `GERAL`                          |
| Integração: ordem correta com múltiplas filas                      | Ordem esperada respeitada                |

> Não há teste cobrindo `dataNascimento = null` em `isPriorityGroup` — o helper `criarPaciente()` usado pelos testes sempre define `dataNascimento`. O código trata esse caso (`PriorityCalculator.java`, retorna `false` sem NPE), mas não há teste unitário exercitando-o.

---

*Documento atualizado em: julho/2026 — revisado contra `PriorityCalculator.java` e `QueueEntryJpaRepository.java` reais.*
