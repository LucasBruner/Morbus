package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record SolicitacaoCreatedResponseDTO(UUID id,
                                            UUID pacienteId,
                                            UUID procedureId,
                                            UUID unidadeSolicitanteId,
                                            EStatusSolicitacao status,
                                            ERiscoSolicitado riscoSolicitado,
                                            String observacoes,
                                            UUID solicitadoPor,
                                            LocalDateTime createdAt) {

    public static SolicitacaoCreatedResponseDTO fromDomain(Solicitacao s) {
        return new SolicitacaoCreatedResponseDTO(
                s.getId(),
                s.getPacienteId(),
                s.getProcedureId(),
                s.getUnidadeSolicitanteId(),
                s.getStatus(),
                s.getRiscoSolicitado(),
                s.getObservacoes(),
                s.getSolicitadoPor(),
                s.getCreatedAt()
        );
    }
}
