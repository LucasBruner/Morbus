package br.com.morbus.regulacao.ports.in;

import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.dto.ListarSolicitacoesQuery;
import org.springframework.data.domain.Page;

public interface IListarSolicitacoesUseCase {
    Page<Solicitacao> execute (ListarSolicitacoesQuery query);
}
