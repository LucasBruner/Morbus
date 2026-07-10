package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.ports.in.dto.ComplementarSolicitacaoCommand;

import java.util.UUID;

public record ComplementarSolicitacaoRequestDTO(
        String cid,
        String justificativaClinica,
        String profissionalSolicitante,
        String crmProfissional) {

    public ComplementarSolicitacaoCommand toCommand(UUID solicitacaoId) {
        return new ComplementarSolicitacaoCommand(solicitacaoId, cid, justificativaClinica,
                profissionalSolicitante, crmProfissional);
    }
}
