package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.entity.Procedure;

public record QueuePositionProcedureDTO(String coProcedimento, String noProcedimento) {
    public static QueuePositionProcedureDTO fromEntity(Procedure procedure) {
        if (procedure == null) return null;
        return new QueuePositionProcedureDTO(procedure.getCoProcedimento(), procedure.getNoProcedimento());
    }
}
