package br.com.morbus.regulacao.ports.in;

import br.com.morbus.regulacao.domain.dto.ListarSolicitacoesQuery;
import br.com.morbus.regulacao.domain.dto.PageResult;
import br.com.morbus.regulacao.domain.model.Solicitacao;

public interface IListarSolicitacoesUseCase {
    PageResult<Solicitacao> execute(ListarSolicitacoesQuery query);
}
