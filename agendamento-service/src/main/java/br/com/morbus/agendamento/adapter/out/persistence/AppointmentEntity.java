package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.enums.EAppointmentStatus;
import br.com.morbus.agendamento.domain.model.Appointment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments", schema = "agendamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "queue_entry_id", nullable = false)
    private UUID queueEntryId;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EAppointmentStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static AppointmentEntity fromDomain(Appointment appointment) {
        return new AppointmentEntity(
                appointment.getId(),
                appointment.getQueueEntryId(),
                appointment.getSlotId(),
                appointment.getPatientId(),
                appointment.getStatus(),
                appointment.getExpiresAt(),
                appointment.getCancellationReason(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }

    public Appointment toDomain() {
        return new Appointment(
                id,
                queueEntryId,
                slotId,
                patientId,
                status,
                expiresAt,
                cancellationReason,
                createdAt,
                updatedAt
        );
    }
}
