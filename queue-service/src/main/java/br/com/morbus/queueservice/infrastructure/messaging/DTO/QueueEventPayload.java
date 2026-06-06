package br.com.morbus.queueservice.infrastructure.messaging.DTO;

import br.com.morbus.queueservice.domain.enums.ERiskColor;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

public record QueueEventPayload(String eventType,
                                UUID queueEntryId,
                                String patientName,
                                String patientContact,
                                String procedureName,
                                ERiskColor riskColor,
                                @Nullable String motivoCancelamento,
                                LocalDateTime timestamp) {
}
