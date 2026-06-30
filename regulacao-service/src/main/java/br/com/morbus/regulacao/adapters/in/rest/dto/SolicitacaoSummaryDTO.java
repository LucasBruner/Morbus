package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record SolicitacaoSummaryDTO(UUID id,
                                    UUID patientId,
                                    UUID procedureId,
                                    UUID unitSolicitanteId,
                                    EStatusSolicitacao status,
                                    ERiscoSolicitado riskColor,
                                    EDestino destino,
                                    LocalDateTime criadaEm) {

    public static SolicitacaoSummaryDTO fromDomain(Solicitacao s) {
        return new SolicitacaoSummaryDTO(
                s.getId(),
                s.getPatientId(),
                s.getProcedureId(),
                s.getUnidadeSolicitanteId(),
                s.getStatus(),
                s.getRiskColor(),
                s.getDestino(),
                s.getCreatedAt());
    }
}
