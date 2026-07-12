package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.UnitProcedureQuota;
import br.com.morbus.queueservice.domain.repository.IUnitProcedureQuotaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public class CreateOrUpdateQuota {

    private final IUnitProcedureQuotaRepository quotaRepository;

    private CreateOrUpdateQuota(IUnitProcedureQuotaRepository quotaRepository) {
        this.quotaRepository = quotaRepository;
    }

    public static CreateOrUpdateQuota create(IUnitProcedureQuotaRepository quotaRepository) {
        return new CreateOrUpdateQuota(quotaRepository);
    }

    public UnitProcedureQuota execute(UUID unitId, UUID procedureId, int maxPerDay) {
        LocalDateTime now = LocalDateTime.now();
        UnitProcedureQuota existing = quotaRepository.findByUnitAndProcedure(unitId, procedureId).orElse(null);

        UnitProcedureQuota quota = UnitProcedureQuota.builder()
                .id(existing != null ? existing.getId() : UUID.randomUUID())
                .unitId(unitId)
                .procedureId(procedureId)
                .maxPerDay(maxPerDay)
                .createdAt(existing != null ? existing.getCreatedAt() : now)
                .updatedAt(now)
                .build();

        quotaRepository.save(quota);
        return quota;
    }
}
