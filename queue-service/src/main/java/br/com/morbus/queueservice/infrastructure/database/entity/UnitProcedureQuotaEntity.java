package br.com.morbus.queueservice.infrastructure.database.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "unit_procedure_quotas")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UnitProcedureQuotaEntity {

    @Id
    private UUID id;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "procedure_id", nullable = false)
    private UUID procedureId;

    @Column(name = "max_per_day", nullable = false)
    private int maxPerDay;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
