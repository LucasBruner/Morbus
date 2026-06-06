package br.com.sus.notification.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record QueueEventDTO(String eventType,
                            UUID queueEntryId,
                            String patientName,
                            String patientContact,
                            String procedureName,
                            String riskColor,
                            LocalDateTime timestamp) {
}
