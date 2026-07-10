package br.com.morbus.regulacao.ports.in.dto;

import java.util.UUID;

public record ComplementarSolicitacaoCommand(UUID solicitacaoId,
                                              String cid,
                                              String justificativaClinica,
                                              String profissionalSolicitante,
                                              String crmProfissional) {
}
