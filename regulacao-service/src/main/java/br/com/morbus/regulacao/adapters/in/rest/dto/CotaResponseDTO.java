package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.model.Quota;

import java.time.LocalDate;
import java.util.UUID;

public record CotaResponseDTO(UUID id,
                              UUID unitId,
                              UUID procedureId,
                              int maxPerPeriod,
                              int currentCount,
                              LocalDate periodStart) {

    public static CotaResponseDTO fromResult(Quota cota) {
        return new CotaResponseDTO(cota.getId(),
                cota.getUnitId(),
                cota.getProcedureId(),
                cota.getMaxPerPeriod(),
                cota.getCurrentCount(),
                cota.getPeriodStart());
    }
}
