package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.entity.UnitProcedureQuota;

import java.util.UUID;

public record QuotaResponseDTO(
        UUID id,
        UUID unitId,
        UUID procedureId,
        int maxPerDay
) {
    public static QuotaResponseDTO fromEntity(UnitProcedureQuota quota) {
        return new QuotaResponseDTO(quota.getId(), quota.getUnitId(), quota.getProcedureId(), quota.getMaxPerDay());
    }
}
