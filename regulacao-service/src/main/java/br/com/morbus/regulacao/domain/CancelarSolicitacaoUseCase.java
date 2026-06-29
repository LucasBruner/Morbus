package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoPendenteException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.ICancelarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;

import java.util.UUID;

public class CancelarSolicitacaoUseCase implements ICancelarSolicitacaoUseCase {
    private final ISolicitacaoRepository solicitacaoRepository;

    public CancelarSolicitacaoUseCase(ISolicitacaoRepository solicitacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
    }

    @Override
    public void execute(UUID idSolicitacao) {
        Solicitacao solicitacao = solicitacaoRepository.findById(idSolicitacao);

        if(!solicitacao.getStatus().equals(EStatusSolicitacao.PENDENTE))
            throw new SolicitacaoNaoPendenteException("A solicitação informada não está com status PENDENTE.");

        solicitacao.cancelar();
        solicitacaoRepository.save(solicitacao);
    }
}
