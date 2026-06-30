package br.com.morbus.agendamento.domain.model;

import br.com.morbus.agendamento.domain.enums.EAppointmentStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Appointment {

    private final UUID id;
    private final UUID queueEntryId;
    private final UUID slotId;
    private final UUID patientId;
    private final EAppointmentStatus status;
    private final LocalDateTime expiresAt;
    private final String cancellationReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Appointment(UUID queueEntryId,
                       UUID slotId,
                       UUID patientId,
                       LocalDateTime expiresAt) {
        this(
                UUID.randomUUID(),
                queueEntryId,
                slotId,
                patientId,
                EAppointmentStatus.AGUARDANDO_CONFIRMACAO,
                expiresAt,
                null,
                LocalDateTime.now(),
                null
        );
    }

    public Appointment(UUID id,
                       UUID queueEntryId,
                       UUID slotId,
                       UUID patientId,
                       EAppointmentStatus status,
                       LocalDateTime expiresAt,
                       String cancellationReason,
                       LocalDateTime createdAt,
                       LocalDateTime updatedAt) {
        this.id = id;
        this.queueEntryId = queueEntryId;
        this.slotId = slotId;
        this.patientId = patientId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
