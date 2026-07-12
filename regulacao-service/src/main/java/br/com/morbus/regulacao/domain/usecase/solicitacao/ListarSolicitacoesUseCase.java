package br.com.morbus.regulacao.domain.usecase.solicitacao;

import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.IListarSolicitacoesUseCase;
import br.com.morbus.regulacao.domain.dto.ListarSolicitacoesQuery;
import br.com.morbus.regulacao.domain.dto.PageResult;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;

public class ListarSolicitacoesUseCase implements IListarSolicitacoesUseCase {
    private final ISolicitacaoRepository repository;

    public ListarSolicitacoesUseCase(ISolicitacaoRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<Solicitacao> execute(ListarSolicitacoesQuery query) {
        return repository.listar(query);
    }
}
