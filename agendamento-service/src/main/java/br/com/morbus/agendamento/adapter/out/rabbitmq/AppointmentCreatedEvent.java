package br.com.morbus.agendamento.adapter.out.rabbitmq;

import java.util.UUID;

public record AppointmentCreatedEvent(
        UUID solicitacaoId,
        UUID appointmentId,
        UUID slotId
) {
}
