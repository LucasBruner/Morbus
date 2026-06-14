package br.com.morbus.queueservice.infrastructure.database.entity;

import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "queue_entries")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueueEntryEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "risk_color", nullable = false)
    private ERiskColor riskColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EQueueStatus status;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedure_id", nullable = false)
    private ProcedureEntity procedure;
}
