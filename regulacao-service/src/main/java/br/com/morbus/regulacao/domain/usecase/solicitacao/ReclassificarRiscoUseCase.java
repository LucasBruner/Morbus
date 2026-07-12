package br.com.morbus.regulacao.domain.usecase.solicitacao;

import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoPendenteException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.IReclassificarRiscoUseCase;
import br.com.morbus.regulacao.ports.in.dto.ReclassificarRiscoCommand;
import br.com.morbus.regulacao.ports.out.IRegulacaoEventPublisher;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;

public class ReclassificarRiscoUseCase implements IReclassificarRiscoUseCase {

    private final ISolicitacaoRepository solicitacaoRepository;
    private final IRegulacaoEventPublisher eventPublisher;

    public ReclassificarRiscoUseCase(ISolicitacaoRepository solicitacaoRepository,
                                     IRegulacaoEventPublisher eventPublisher) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Solicitacao execute(ReclassificarRiscoCommand command) {
        Solicitacao solicitacao = solicitacaoRepository.findById(command.solicitacaoId());

        if (solicitacao.getStatus() != EStatusSolicitacao.APROVADA) {
            throw new SolicitacaoNaoPendenteException(
                    "A solicitacao deve estar com status APROVADA para ser reclassificada.");
        }

        if (solicitacao.getDestino() == EDestino.FILA_ESPERA) {
            throw new SolicitacaoNaoPendenteException(
                    "Solicitacoes em FILA_ESPERA nao podem ser reclassificadas.");
        }

        solicitacao.reclassificarRisco(command.riskColor());
        Solicitacao solicitacaoSalva = solicitacaoRepository.save(solicitacao);

        eventPublisher.publishSolicitacaoReclassificada(solicitacaoSalva);

        return solicitacaoSalva;
    }
}
