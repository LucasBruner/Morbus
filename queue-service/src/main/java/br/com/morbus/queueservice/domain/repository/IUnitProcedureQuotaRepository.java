package br.com.morbus.queueservice.domain.repository;

import br.com.morbus.queueservice.domain.entity.UnitProcedureQuota;

import java.util.Optional;
import java.util.UUID;

public interface IUnitProcedureQuotaRepository {
    void save(UnitProcedureQuota quota);
    Optional<UnitProcedureQuota> findByUnitAndProcedure(UUID unitId, UUID procedureId);
}
