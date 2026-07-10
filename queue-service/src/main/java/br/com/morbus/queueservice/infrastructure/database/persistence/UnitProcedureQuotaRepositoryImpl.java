package br.com.morbus.queueservice.infrastructure.database.persistence;

import br.com.morbus.queueservice.domain.entity.UnitProcedureQuota;
import br.com.morbus.queueservice.domain.repository.IUnitProcedureQuotaRepository;
import br.com.morbus.queueservice.infrastructure.database.entity.UnitProcedureQuotaEntity;
import br.com.morbus.queueservice.infrastructure.database.repository.UnitProcedureQuotaJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UnitProcedureQuotaRepositoryImpl implements IUnitProcedureQuotaRepository {

    private final UnitProcedureQuotaJpaRepository repository;

    public UnitProcedureQuotaRepositoryImpl(UnitProcedureQuotaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(UnitProcedureQuota quota) {
        UnitProcedureQuotaEntity entity = UnitProcedureQuotaEntity.builder()
                .id(quota.getId())
                .unitId(quota.getUnitId())
                .procedureId(quota.getProcedureId())
                .maxPerDay(quota.getMaxPerDay())
                .createdAt(quota.getCreatedAt())
                .updatedAt(quota.getUpdatedAt())
                .build();
        repository.save(entity);
    }

    @Override
    public Optional<UnitProcedureQuota> findByUnitAndProcedure(UUID unitId, UUID procedureId) {
        return repository.findByUnitIdAndProcedureId(unitId, procedureId)
                .map(this::mapToDomain);
    }

    private UnitProcedureQuota mapToDomain(UnitProcedureQuotaEntity entity) {
        return UnitProcedureQuota.builder()
                .id(entity.getId())
                .unitId(entity.getUnitId())
                .procedureId(entity.getProcedureId())
                .maxPerDay(entity.getMaxPerDay())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
