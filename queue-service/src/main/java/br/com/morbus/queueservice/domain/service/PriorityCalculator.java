package br.com.morbus.queueservice.domain.service;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;

import java.time.LocalDate;

/**
 * Calculadora de priorização de fila conforme as regras do SUS.
 * 
 * Critérios de ordenação:
 * 1. Cor clínica (riskColor.numericPriority ASC)
 * 2. Grupo legal/prioridade (priorityGroup.numericPriority ASC)
 * 3. Timestamp de entrada (registeredAt ASC)
 */
public class PriorityCalculator {
    
    private static final int IDOSO_AGE_THRESHOLD = 60;
    
    /**
     * Compara duas entradas de fila conforme as regras do SUS:
     * 1. Cor clínica (riskColor.numericPriority ASC - vermelho=1 primeiro)
     * 2. Grupo legal (priorityGroup.numericPriority ASC - IDOSO=1 primeiro)
     * 3. Timestamp de entrada (registeredAt ASC - mais antigo primeiro)
     * 
     * @param a Primeira entrada de fila
     * @param b Segunda entrada de fila
     * @return Inteiro negativo se a tem prioridade maior que b,
     *         inteiro positivo se b tem prioridade maior que a,
     *         zero se têm a mesma prioridade
     */
    public static int compare(QueueEntry a, QueueEntry b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        
        // Comparar por riskColor (1 = vermelho primeiro)
        int riskComparison = Integer.compare(
            a.getRiskColor().getNumericPriority(),
            b.getRiskColor().getNumericPriority()
        );
        if (riskComparison != 0) {
            return riskComparison;
        }
        
        // Comparar por priorityGroup
        int priorityGroupComparison = Integer.compare(
            getPriorityGroup(a.getPatient()).getNumericPriority(),
            getPriorityGroup(b.getPatient()).getNumericPriority()
        );
        if (priorityGroupComparison != 0) {
            return priorityGroupComparison;
        }
        
        // Desempate: comparar por timestamp (mais antigo primeiro)
        return a.getRegisteredAt().compareTo(b.getRegisteredAt());
    }
    
    /**
     * Obtém o grupo de prioridade efetivo do paciente:
     * Se for idoso (60+), retorna IDOSO
     * Caso contrário, retorna o grupoLegal configurado ou GERAL
     * 
     * @param patient Paciente a ser verificado
     * @return EPriorityGroup baseado na idade ou grupo legal
     */
    public static EPriorityGroup getPriorityGroup(Patient patient) {
        if (patient == null) {
            return EPriorityGroup.GERAL;
        }
        
        if (isPriorityGroup(patient)) {
            return EPriorityGroup.IDOSO;
        }
        
        EPriorityGroup grupoLegal = patient.getGrupoLegal();
        return grupoLegal != null ? grupoLegal : EPriorityGroup.GERAL;
    }
    
    /**
     * Verifica se o paciente tem 60+ anos (IDOSO).
     * Pacientes idosos têm prioridade automática.
     * 
     * @param patient Paciente a ser verificado
     * @return true se o paciente tem 60 anos ou mais, false caso contrário
     */
    public static boolean isPriorityGroup(Patient patient) {
        if (patient == null || patient.getDataNascimento() == null) {
            return false;
        }
        
        LocalDate birthDate = patient.getDataNascimento();
        LocalDate today = LocalDate.now();
        int age = today.getYear() - birthDate.getYear();
        
        // Ajustar idade se o aniversário ainda não passou este ano
        if (today.getMonthValue() < birthDate.getMonthValue() ||
            (today.getMonthValue() == birthDate.getMonthValue() && 
             today.getDayOfMonth() < birthDate.getDayOfMonth())) {
            age--;
        }
        
        return age >= IDOSO_AGE_THRESHOLD;
    }
}
