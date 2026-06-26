package br.com.morbus.regulacao.domain;

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
        if (solicitacaoRepository.existsAtiva(command.pacienteId(), command.procedureId())) {
            throw new DuplicateSolicitacaoException(
                    "Já existe solicitação PENDENTE ou APROVADA para pacienteId=%s e procedureId=%s"
                            .formatted(command.pacienteId(), command.procedureId()));
        }

        Solicitacao solicitacao = new Solicitacao(
                command.pacienteId(),
                command.procedureId(),
                command.unidadeSolicitanteId(),
                command.riscoSolicitado(),
                command.observacoes(),
                command.solicitadoPor()
        );

        return solicitacaoRepository.save(solicitacao);
    }
}
