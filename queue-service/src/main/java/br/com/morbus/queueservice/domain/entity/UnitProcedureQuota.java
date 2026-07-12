package br.com.morbus.queueservice.domain.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
public class UnitProcedureQuota {
    private UUID id;
    private UUID unitId;
    private UUID procedureId;
    private int maxPerDay;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
