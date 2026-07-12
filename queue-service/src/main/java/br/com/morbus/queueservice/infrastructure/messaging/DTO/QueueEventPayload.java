package br.com.morbus.queueservice.infrastructure.messaging.DTO;

import br.com.morbus.queueservice.domain.enums.EDestino;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

public record QueueEventPayload(String eventType,
                                UUID queueEntryId,
                                UUID patientId,
                                String patientName,
                                String patientContact,
                                String procedureName,
                                UUID procedureId,
                                @Nullable UUID preferredUnitId,
                                ERiskColor riskColor,
                                EDestino tipoFila,
                                @Nullable String motivoCancelamento,
                                LocalDateTime timestamp) {
}
