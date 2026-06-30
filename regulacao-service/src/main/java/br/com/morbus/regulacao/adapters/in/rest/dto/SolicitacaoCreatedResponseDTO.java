package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record SolicitacaoCreatedResponseDTO(UUID id,
                                            UUID patientId,
                                            UUID procedureId,
                                            String cid,
                                            EDestino destino,
                                            ERiscoSolicitado riskColor,
                                            EStatusSolicitacao status,
                                            LocalDateTime criadaEm) {

    public static SolicitacaoCreatedResponseDTO fromDomain(Solicitacao s) {
        return new SolicitacaoCreatedResponseDTO(
                s.getId(),
                s.getPatientId(),
                s.getProcedureId(),
                s.getCid(),
                s.getDestino(),
                s.getRiskColor(),
                s.getStatus(),
                s.getCreatedAt()
        );
    }
}
