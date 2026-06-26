package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record SolicitacaoSummaryDTO(UUID id,
                                    UUID pacienteId,
                                    UUID procedureId,
                                    UUID unidadeSolicitanteId,
                                    EStatusSolicitacao status,
                                    ERiscoSolicitado riscoSolicitado,
                                    LocalDateTime createdAt) {
    public static SolicitacaoSummaryDTO fromDomain(Solicitacao s) {
        return new SolicitacaoSummaryDTO(s.getId(),
                s.getPacienteId(),
                s.getProcedureId(),
                s.getUnidadeSolicitanteId(),
                s.getStatus(),
                s.getRiscoSolicitado(),
                s.getCreatedAt());
    }
}
