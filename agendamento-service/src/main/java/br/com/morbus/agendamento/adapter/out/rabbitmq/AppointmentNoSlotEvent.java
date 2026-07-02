package br.com.morbus.agendamento.adapter.out.rabbitmq;

import java.util.UUID;

public record AppointmentNoSlotEvent(
        UUID queueEntryId,
        UUID patientId,
        UUID procedureId
) {
}
