package br.com.morbus.regulacao.ports.in.dto;

import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;

import java.util.UUID;

public record ListarSolicitacoesQuery(UUID unidadeId,
                                      EStatusSolicitacao status,
                                      UUID procedureId,
                                      int page,
                                      int size) {
}
