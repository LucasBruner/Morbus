package br.com.morbus.queueservice.infrastructure.messaging.consumer;

import java.util.UUID;

public record AppointmentNoSlotEvent(UUID queueEntryId,
                                     UUID patientId,
                                     UUID procedureId) {
}
