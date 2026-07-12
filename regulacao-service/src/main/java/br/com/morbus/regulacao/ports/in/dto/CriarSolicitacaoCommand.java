package br.com.morbus.regulacao.ports.in.dto;

import br.com.morbus.regulacao.domain.enums.EDestino;

import java.util.UUID;

public record CriarSolicitacaoCommand(UUID patientId,
                                      UUID procedureId,
                                      UUID unidadeSolicitanteId,
                                      String cid,
                                      String justificativaClinica,
                                      String profissionalSolicitante,
                                      String crmProfissional,
                                      EDestino destino,
                                      UUID solicitadoPor,
                                      String observacoes) {
}
