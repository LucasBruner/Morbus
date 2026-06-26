package br.com.morbus.regulacao.ports.out;

import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.dto.ListarSolicitacoesQuery;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ISolicitacaoRepository {
    boolean existsAtiva(UUID pacienteId, UUID procedureId);
    Solicitacao save(Solicitacao solicitacao);
    Page<Solicitacao> listar(ListarSolicitacoesQuery query);
}
