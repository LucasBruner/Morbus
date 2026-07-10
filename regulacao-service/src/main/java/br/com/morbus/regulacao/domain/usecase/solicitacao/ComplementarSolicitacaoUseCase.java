package br.com.morbus.regulacao.domain.usecase.solicitacao;

import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoPendenteException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.IComplementarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.dto.ComplementarSolicitacaoCommand;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;

public class ComplementarSolicitacaoUseCase implements IComplementarSolicitacaoUseCase {

    private final ISolicitacaoRepository solicitacaoRepository;

    public ComplementarSolicitacaoUseCase(ISolicitacaoRepository solicitacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
    }

    @Override
    public Solicitacao execute(ComplementarSolicitacaoCommand command) {
        Solicitacao solicitacao = solicitacaoRepository.findById(command.solicitacaoId());

        if (solicitacao.getStatus() != EStatusSolicitacao.DEVOLVIDA) {
            throw new SolicitacaoNaoPendenteException(
                    "A solicitacao deve estar com status DEVOLVIDA para ser complementada.");
        }

        solicitacao.complementar(command.cid(), command.justificativaClinica(),
                command.profissionalSolicitante(), command.crmProfissional());

        return solicitacaoRepository.save(solicitacao);
    }
}
