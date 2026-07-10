# Implementação: Lógica de Priorização de Fila SUS

## Resumo

O `PriorityCalculator` implementa o algoritmo de ordenação da fila ambulatorial do SUS, com suporte a dois tipos de fila — `FILA_REGULADA` e `FILA_ESPERA` — que seguem regras de ordenação distintas.

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

```java
public static int compare(QueueEntry a, QueueEntry b) {

    // Critério 1: FILA_REGULADA (0) precede FILA_ESPERA (1)
    int tipoComp = a.getTipoFila().ordinal() - b.getTipoFila().ordinal();
    if (tipoComp != 0) return tipoComp;

    // Critérios 2 e 3: apenas dentro de FILA_REGULADA
    if (a.getTipoFila() == EDestino.FILA_REGULADA) {

        // Critério 2: cor de risco
        int colorComp = a.getRiskColor().getNumericPriority()
                      - b.getRiskColor().getNumericPriority();
        if (colorComp != 0) return colorComp;

        // Critério 3: grupo de prioridade
        int groupComp = getPriorityGroup(a.getPatient()).getNumericPriority()
                      - getPriorityGroup(b.getPatient()).getNumericPriority();
        if (groupComp != 0) return groupComp;
    }

    // Critério 4: timestamp de chegada (desempate final — válido para ambos os tipos)
    return a.getRegisteredAt().compareTo(b.getRegisteredAt());
}
```

### getPriorityGroup(Patient patient)

Retorna o grupo de prioridade efetivo do paciente. Idosos (≥ 60 anos) recebem `IDOSO` automaticamente.

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

```java
List<QueueEntry> fila = repository.findAll();
fila.sort(PriorityCalculator::compare);
// Resultado: FILA_REGULADA primeiro, depois FILA_ESPERA
// Dentro de cada tipo: regras específicas se aplicam
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

Após `fila.sort(PriorityCalculator::compare)`:

| Pos. | Paciente         | Motivo                                                      |
|------|------------------|-------------------------------------------------------------|
| 1    | Ana              | FILA_REGULADA + VERMELHO (prioridade máxima)                |
| 2    | Pedro (65 anos)  | FILA_REGULADA + AMARELO + IDOSO (≥ 60 anos)                 |
| 3    | Joana            | FILA_REGULADA + AMARELO + GERAL + chegou antes que... (não há outro AMARELO+GERAL) |
| 4    | Carlos (72 anos) | FILA_ESPERA + chegou antes (01/01) — grupo ignorado         |
| 5    | Marcos           | FILA_ESPERA + chegou depois (04/01)                         |

> Carlos tem 72 anos mas está em `FILA_ESPERA`, onde somente o timestamp importa. Ele não ocupa posição 1 apenas por ser idoso — toda `FILA_REGULADA` precede `FILA_ESPERA`.

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

*Documento atualizado em: junho/2026*
