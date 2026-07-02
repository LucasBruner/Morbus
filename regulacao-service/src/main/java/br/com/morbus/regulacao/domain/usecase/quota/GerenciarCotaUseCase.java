package br.com.morbus.regulacao.domain.usecase.quota;

import br.com.morbus.regulacao.domain.exception.UnidadeSolicitanteNaoEncontradaException;
import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.ports.in.IGerenciarCotaUseCase;
import br.com.morbus.regulacao.ports.in.dto.GerenciarCotaCommand;
import br.com.morbus.regulacao.ports.out.IQuotaRepository;
import br.com.morbus.regulacao.ports.out.IUnidadeSolicitanteRepository;

import java.util.Optional;

public class GerenciarCotaUseCase implements IGerenciarCotaUseCase {

    private final IQuotaRepository quotaRepository;
    private final IUnidadeSolicitanteRepository unidadeSolicitanteRepository;

    public GerenciarCotaUseCase(IQuotaRepository quotaRepository, IUnidadeSolicitanteRepository unidadeSolicitanteRepository) {
        this.quotaRepository = quotaRepository;
        this.unidadeSolicitanteRepository = unidadeSolicitanteRepository;
    }


    @Override
    public Quota execute(GerenciarCotaCommand command) {
        if (!unidadeSolicitanteRepository.existsById(command.unitId())) {
            throw new UnidadeSolicitanteNaoEncontradaException(
                    "Id informado nao possui nenhuma unidade solicitante cadastrada: %s".formatted(command.unitId()));
        }

        Optional<Quota> existente = quotaRepository.buscarPorChave(command.unitId(),
                command.procedureId(),
                command.periodStart());

        Quota quota = existente.map(q -> {
                    q.alterarLimite(command.maxPerPeriod());
                    return q;
                })
                .orElseGet(() -> Quota.criar(command.unitId(),
                        command.procedureId(),
                        command.maxPerPeriod(),
                        command.periodStart()));

        return quotaRepository.salvar(quota);
    }
}
