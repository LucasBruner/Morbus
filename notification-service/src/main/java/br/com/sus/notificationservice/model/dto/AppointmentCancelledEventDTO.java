package br.com.sus.notificationservice.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentCancelledEventDTO(UUID appointmentId,
                                           UUID queueEntryId,
                                           UUID patientId,
                                           String motivo,
                                           LocalDateTime canceladoEm) {
}
