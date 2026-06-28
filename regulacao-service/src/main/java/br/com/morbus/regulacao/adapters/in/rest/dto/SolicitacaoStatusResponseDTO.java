package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record SolicitacaoStatusResponseDTO(UUID id,
                                           UUID pacienteId,
                                           UUID procedureId,
                                           EStatusSolicitacao statusSolicitacao,
                                           ERiscoSolicitado riscoSolicitado,
                                           LocalDateTime createdAt,
                                           LocalDateTime updatedAt,
                                           String parecer) {
    public static SolicitacaoStatusResponseDTO fromDomain (Solicitacao solicitacao) {
        return new SolicitacaoStatusResponseDTO(solicitacao.getId(),
                solicitacao.getPacienteId(),
                solicitacao.getProcedureId(),
                solicitacao.getStatus(),
                solicitacao.getRiscoSolicitado(),
                solicitacao.getCreatedAt(),
                solicitacao.getUpdatedAt(),
                solicitacao.getObservacoes()
        );
    }
}
