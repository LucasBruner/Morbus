package br.com.morbus.regulacao.domain.dto;

import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;

import java.util.UUID;

public record ListarSolicitacoesQuery(UUID unidadeId,
                                      EStatusSolicitacao status,
                                      EDestino destino,
                                      UUID procedureId,
                                      int page,
                                      int size,
                                      ESortDirection sortDirection) {

    public ListarSolicitacoesQuery(UUID unidadeId, EStatusSolicitacao status, UUID procedureId, int page, int size) {
        this(unidadeId, status, null, procedureId, page, size, ESortDirection.DESC);
    }

    public ListarSolicitacoesQuery(UUID unidadeId, EStatusSolicitacao status, UUID procedureId, int page, int size, ESortDirection sortDirection) {
        this(unidadeId, status, null, procedureId, page, size, sortDirection);
    }

    public ListarSolicitacoesQuery(UUID unidadeId, EStatusSolicitacao status, EDestino destino, UUID procedureId, int page, int size) {
        this(unidadeId, status, destino, procedureId, page, size, ESortDirection.DESC);
    }
}
