package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.IListarSolicitacoesUseCase;
import br.com.morbus.regulacao.ports.in.dto.ListarSolicitacoesQuery;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;
import org.springframework.data.domain.Page;

public class ListarSolicitacoesUseCase implements IListarSolicitacoesUseCase {
    private final ISolicitacaoRepository repository;

    public ListarSolicitacoesUseCase(ISolicitacaoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<Solicitacao> execute(ListarSolicitacoesQuery query) {
        return repository.listar(query);
    }
}
