package br.com.morbus.regulacao.domain.model;

import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
public class Quota {
    private UUID id;
    private UUID unitId;
    private UUID procedureId;
    private int maxPerPeriod;
    private int currentCount;
    private LocalDate periodStart;

    public Quota(UUID id, UUID unitId, UUID procedureId, int maxPerPeriod, int currentCount, LocalDate periodStart) {
        this.id = id;
        this.unitId = unitId;
        this.procedureId = procedureId;
        this.maxPerPeriod = maxPerPeriod;
        this.currentCount = currentCount;
        this.periodStart = periodStart;
    }

    public static Quota bloqueada (UUID unitId,
                                   UUID procedureId,
                                   LocalDate periodStart){
        return new Quota(UUID.randomUUID(),
                unitId,
                procedureId,
                0,
                0,
                periodStart);
    }
}
