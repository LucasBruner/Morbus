package br.com.sus.notificationservice.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentRescheduledEventDTO(UUID appointmentId,
                                             UUID slotId,
                                             UUID queueEntryId,
                                             UUID patientId,
                                             LocalDateTime reagendadoEm) {
}
