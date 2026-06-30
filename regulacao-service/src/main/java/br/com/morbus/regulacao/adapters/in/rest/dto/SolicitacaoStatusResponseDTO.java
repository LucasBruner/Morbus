package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record SolicitacaoStatusResponseDTO(UUID id,
                                           UUID patientId,
                                           UUID procedureId,
                                           String cid,
                                           String justificativaClinica,
                                           String profissionalSolicitante,
                                           EDestino destino,
                                           ERiscoSolicitado riskColor,
                                           EStatusSolicitacao status,
                                           LocalDateTime criadaEm,
                                           LocalDateTime updatedAt,
                                           String justificativaNegacao) {

    public static SolicitacaoStatusResponseDTO fromDomain(Solicitacao solicitacao) {
        return new SolicitacaoStatusResponseDTO(
                solicitacao.getId(),
                solicitacao.getPatientId(),
                solicitacao.getProcedureId(),
                solicitacao.getCid(),
                solicitacao.getJustificativaClinica(),
                solicitacao.getProfissionalSolicitante(),
                solicitacao.getDestino(),
                solicitacao.getRiskColor(),
                solicitacao.getStatus(),
                solicitacao.getCreatedAt(),
                solicitacao.getUpdatedAt(),
                solicitacao.getJustificativaNegacao()
        );
    }
}
