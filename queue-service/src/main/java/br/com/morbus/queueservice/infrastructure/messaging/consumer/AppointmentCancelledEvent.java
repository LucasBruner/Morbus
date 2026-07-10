package br.com.morbus.queueservice.infrastructure.messaging.consumer;

import java.util.UUID;

public record AppointmentCancelledEvent(UUID queueEntryId,
                                        UUID patientId) {
}
