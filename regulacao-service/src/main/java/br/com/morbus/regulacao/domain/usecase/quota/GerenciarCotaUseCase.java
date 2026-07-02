package br.com.morbus.regulacao.domain.usecase.quota;

import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.ports.in.IGerenciarCotaUseCase;
import br.com.morbus.regulacao.ports.in.dto.GerenciarCotaCommand;
import br.com.morbus.regulacao.ports.out.IQuotaRepository;

import java.util.Optional;

public class GerenciarCotaUseCase implements IGerenciarCotaUseCase {

    private final IQuotaRepository quotaRepository;

    public GerenciarCotaUseCase(IQuotaRepository quotaRepository) {
        this.quotaRepository = quotaRepository;
    }


    @Override
    public Quota execute(GerenciarCotaCommand command) {
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
