package br.com.morbus.regulacao.ports.out;

import br.com.morbus.regulacao.domain.dto.ListarSolicitacoesQuery;
import br.com.morbus.regulacao.domain.dto.PageResult;
import br.com.morbus.regulacao.domain.model.Solicitacao;

import java.util.UUID;

public interface ISolicitacaoRepository {
    Solicitacao findById(UUID solicitacaoId);
    boolean existsAtiva(UUID pacienteId, UUID procedureId);
    Solicitacao save(Solicitacao solicitacao);
    PageResult<Solicitacao> listar(ListarSolicitacoesQuery query);
}
