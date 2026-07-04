package br.com.morbus.queueservice.infrastructure.messaging.consumer;

import java.util.UUID;

public record PatientNoShowEvent(UUID queueEntryId,
                                 UUID patientId) {
}
