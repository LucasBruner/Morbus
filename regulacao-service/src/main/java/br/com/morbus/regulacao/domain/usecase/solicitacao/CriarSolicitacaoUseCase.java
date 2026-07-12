package br.com.morbus.regulacao.domain.usecase.solicitacao;

import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.exception.CotaExcedidaException;
import br.com.morbus.regulacao.domain.exception.DuplicateSolicitacaoException;
import br.com.morbus.regulacao.domain.exception.UnidadeSolicitanteNaoEncontradaException;
import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.dto.CriarSolicitacaoCommand;
import br.com.morbus.regulacao.ports.in.ICriarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.out.IQuotaRepository;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;
import br.com.morbus.regulacao.ports.out.IUnidadeSolicitanteRepository;

import java.time.LocalDate;

public class CriarSolicitacaoUseCase implements ICriarSolicitacaoUseCase {

    private final ISolicitacaoRepository solicitacaoRepository;
    private final IQuotaRepository quotaRepository;
    private final IUnidadeSolicitanteRepository unidadeSolicitanteRepository;

    public CriarSolicitacaoUseCase(ISolicitacaoRepository solicitacaoRepository,
                                    IQuotaRepository quotaRepository,
                                    IUnidadeSolicitanteRepository unidadeSolicitanteRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.quotaRepository = quotaRepository;
        this.unidadeSolicitanteRepository = unidadeSolicitanteRepository;
    }

    @Override
    public Solicitacao execute(CriarSolicitacaoCommand command) {
        if (!unidadeSolicitanteRepository.existsById(command.unidadeSolicitanteId())) {
            throw new UnidadeSolicitanteNaoEncontradaException(
                    "Id informado nao possui nenhuma unidade solicitante cadastrada: %s"
                            .formatted(command.unidadeSolicitanteId()));
        }

        if (solicitacaoRepository.existsAtiva(command.patientId(), command.procedureId())) {
            throw new DuplicateSolicitacaoException(
                    "Ja existe solicitacao AGUARDANDO ou APROVADA para patientId=%s e procedureId=%s"
                            .formatted(command.patientId(), command.procedureId()));
        }

        if (EDestino.FILA_ESPERA.equals(command.destino())) {
            LocalDate periodStart = LocalDate.now().withDayOfMonth(1);
            Quota quota = quotaRepository.findOrCreate(
                    command.unidadeSolicitanteId(), command.procedureId(), periodStart);

            if(!quotaRepository.incrementarSeDisponivel(quota.getId())) {
                throw new CotaExcedidaException("Cota esgotada para unitId=%s, procedureId=%s no periodo %s"
                        .formatted(command.unidadeSolicitanteId(), command.procedureId(), periodStart));
            }
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
                command.solicitadoPor(),
                command.observacoes()
        );

        return solicitacaoRepository.save(solicitacao);
    }
}
