package br.com.morbus.queueservice.infrastructure.messaging.DTO;

import br.com.morbus.queueservice.domain.enums.EDestino;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

public record QueueEventPayload(String eventType,
                                UUID queueEntryId,
                                UUID patientId,
                                String patientName,
                                String patientContact,
                                String procedureName,
                                UUID procedureId,
                                @Nullable UUID preferredUnitId,
                                @Nullable UUID solicitacaoId,
                                ERiskColor riskColor,
                                EDestino tipoFila,
                                @Nullable String motivoCancelamento,
                                Instant timestamp) {
}
