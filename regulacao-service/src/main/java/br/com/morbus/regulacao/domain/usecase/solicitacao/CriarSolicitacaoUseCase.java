package br.com.morbus.regulacao.domain.usecase.solicitacao;

import br.com.morbus.regulacao.domain.exception.DuplicateSolicitacaoException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.dto.CriarSolicitacaoCommand;
import br.com.morbus.regulacao.ports.in.ICriarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;

public class CriarSolicitacaoUseCase implements ICriarSolicitacaoUseCase {

    private final ISolicitacaoRepository solicitacaoRepository;

    public CriarSolicitacaoUseCase(ISolicitacaoRepository solicitacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
    }

    @Override
    public Solicitacao execute(CriarSolicitacaoCommand command) {
        if (solicitacaoRepository.existsAtiva(command.patientId(), command.procedureId())) {
            throw new DuplicateSolicitacaoException(
                    "Ja existe solicitacao AGUARDANDO ou APROVADA para patientId=%s e procedureId=%s"
                            .formatted(command.patientId(), command.procedureId()));
        }

        Solicitacao solicitacao = new Solicitacao(
                command.patientId(),
                command.procedureId(),
                command.unidadeSolicitanteId(),
                command.cid(),
                command.justificativaClinica(),
                command.profissionalSolicitante(),
                command.crmProfissional(),
                command.destino(),
                command.solicitadoPor()
        );

        return solicitacaoRepository.save(solicitacao);
    }
}
