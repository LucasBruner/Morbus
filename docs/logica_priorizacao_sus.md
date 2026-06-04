# Implementação: Lógica de Priorização de Fila SUS

## Resumo

Implementação completa da lógica de priorização de fila conforme as regras do SUS, com 6 cenários de testes unitários.

## Regras de Priorização

A ordenação segue 3 critérios em cascata:
1. **Cor Clínica** (riskColor.numericPriority ASC)
   - VERMELHO = 1 (máxima urgência)
   - AMARELO = 2
   - VERDE = 3
   - AZUL = 4 (mínima urgência)

2. **Grupo de Prioridade** (priorityGroup.numericPriority ASC)
   - IDOSO = 1 (pacientes com 60+ anos)
   - GESTANTE = 2
   - DEFICIENTE = 3
   - LACTANTE = 4
   - OBESO = 5
   - GERAL = 6

3. **Timestamp de Entrada** (registeredAt ASC)
   - Paciente que chegou primeiro tem prioridade

### PriorityCalculator.java
**Localização:** `queue-service/src/main/java/br/com/morbus/queueservice/domain/service/`

**Métodos principais:**

- **`compare(QueueEntry a, QueueEntry b): int`**
  - Compara duas entradas de fila
  - Retorna: negativo (a tem prioridade), positivo (b tem prioridade), zero (mesma prioridade)
  - Implementa a ordenação em cascata

- **`getPriorityGroup(Patient patient): EPriorityGroup`**
  - Obtém o grupo de prioridade efetivo do paciente
  - Se o paciente tem 60+ anos, retorna IDOSO automaticamente
  - Caso contrário, retorna o grupoLegal configurado ou GERAL

- **`isPriorityGroup(Patient patient): boolean`**
  - Verifica se o paciente tem 60+ anos (IDOSO)
  - Lida com cálculo correto de idade (considerando mês e dia do aniversário)
  - Trata casos nulos com segurança

## Como Usar

### Ordenar uma fila de pacientes

```java
List<QueueEntry> fila = repository.findAll();

// Ordenar usando PriorityCalculator
fila.sort(PriorityCalculator::compare);
```

### Verificar se paciente é idoso

```java
Patient paciente = repository.findById(id);

if (PriorityCalculator.isPriorityGroup(paciente)) {
    // Aplicar prioridade IDOSO
}
```

### Obter grupo de prioridade efetivo

```java
EPriorityGroup grupo = PriorityCalculator.getPriorityGroup(paciente);
// Retorna IDOSO se tem 60+, caso contrário retorna grupoLegal ou GERAL
```
