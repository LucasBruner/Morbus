package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.UnitProcedureQuota;
import br.com.morbus.queueservice.domain.exception.QuotaExceededException;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.repository.IUnitProcedureQuotaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Aplica o controle de cota diária de FILA_ESPERA por unidade+procedimento.
 * Ausência de cota configurada para a combinação = sem limite (opt-in).
 * FILA_REGULADA nunca passa por aqui.
 */
public class CheckAndEnforceQuota {

    private final IUnitProcedureQuotaRepository quotaRepository;
    private final IQueueEntryRepository queueEntryRepository;

    private CheckAndEnforceQuota(IUnitProcedureQuotaRepository quotaRepository,
                                  IQueueEntryRepository queueEntryRepository) {
        this.quotaRepository = quotaRepository;
        this.queueEntryRepository = queueEntryRepository;
    }

    public static CheckAndEnforceQuota create(IUnitProcedureQuotaRepository quotaRepository,
                                               IQueueEntryRepository queueEntryRepository) {
        return new CheckAndEnforceQuota(quotaRepository, queueEntryRepository);
    }

    public void execute(UUID unitId, UUID procedureId) {
        if (unitId == null) {
            return;
        }

        Optional<UnitProcedureQuota> quota = quotaRepository.findByUnitAndProcedure(unitId, procedureId);
        if (quota.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        int used = queueEntryRepository.countActiveFilaEsperaEntries(
                unitId, procedureId, today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        if (used >= quota.get().getMaxPerDay()) {
            throw new QuotaExceededException(
                    "Cota diária de FILA_ESPERA excedida para este procedimento nesta unidade");
        }
    }
}
