package br.com.sus.notificationservice.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentExpiredEventDTO(UUID appointmentId,
                                         UUID queueEntryId,
                                         UUID patientId,
                                         LocalDateTime expirouEm) {
}
