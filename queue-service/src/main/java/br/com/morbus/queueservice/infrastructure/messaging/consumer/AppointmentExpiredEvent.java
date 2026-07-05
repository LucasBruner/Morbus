package br.com.morbus.queueservice.infrastructure.messaging.consumer;

import java.util.UUID;

public record AppointmentExpiredEvent(UUID queueEntryId,
                                      UUID patientId,
                                      String procedureName) {
}
