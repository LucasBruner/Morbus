package br.com.morbus.queueservice.infrastructure.database.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "patient_procedures")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PatientProcedureEntity {

    @EmbeddedId
    private PatientProcedureId id;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    protected void prePersist() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
}
