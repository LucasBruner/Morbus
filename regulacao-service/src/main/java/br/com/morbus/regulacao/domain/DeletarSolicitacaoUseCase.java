package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoPendenteException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.IDeletarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;

import java.util.UUID;

public class DeletarSolicitacaoUseCase implements IDeletarSolicitacaoUseCase {
    private final ISolicitacaoRepository solicitacaoRepository;

    public DeletarSolicitacaoUseCase(ISolicitacaoRepository solicitacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
    }

    @Override
    public void execute(UUID idSolicitacao) {
        Solicitacao solicitacao = solicitacaoRepository.findById(idSolicitacao);

        if(!solicitacao.getStatus().equals(EStatusSolicitacao.PENDENTE))
            throw new SolicitacaoNaoPendenteException("A solicitação informada não está com status de pendênte");

        solicitacaoRepository.delete(solicitacao);
    }
}
