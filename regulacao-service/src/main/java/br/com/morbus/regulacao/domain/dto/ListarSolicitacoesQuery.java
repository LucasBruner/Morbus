package br.com.morbus.regulacao.domain.dto;

import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import org.springframework.data.domain.Sort;

import java.util.UUID;

public record ListarSolicitacoesQuery(UUID unidadeId,
                                      EStatusSolicitacao status,
                                      UUID procedureId,
                                      int page,
                                      int size,
                                      Sort.Direction sortDirection) {

    public ListarSolicitacoesQuery(UUID unidadeId, EStatusSolicitacao status, UUID procedureId, int page, int size) {
        this(unidadeId, status, procedureId, page, size, Sort.Direction.DESC);
    }
}
