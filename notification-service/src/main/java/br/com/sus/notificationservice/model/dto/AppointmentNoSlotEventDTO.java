package br.com.sus.notificationservice.model.dto;

import java.util.UUID;

public record AppointmentNoSlotEventDTO(UUID queueEntryId,
                                        UUID patientId,
                                        UUID procedureId) {
}
