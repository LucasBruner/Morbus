package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.entity.Procedure;
import java.util.UUID;

public record ProcedureResponseDTO(
        UUID id,
        String coProcedimento,
        String noProcedimento,
        int idadeMinima,
        int idadeMaxima,
        String grupo
) {
    public static ProcedureResponseDTO fromEntity(Procedure procedure) {
        return new ProcedureResponseDTO(
                procedure.getId(),
                procedure.getCoProcedimento(),
                procedure.getNoProcedimento(),
                procedure.getIdadeMinima(),
                procedure.getIdadeMaxima(),
                procedure.getGrupo()
        );
    }
}