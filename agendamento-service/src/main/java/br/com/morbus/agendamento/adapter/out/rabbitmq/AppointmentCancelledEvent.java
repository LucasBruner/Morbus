package br.com.morbus.agendamento.adapter.out.rabbitmq;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentCancelledEvent(
        UUID appointmentId,
        UUID queueEntryId,
        UUID patientId,
        String motivo,
        LocalDateTime canceladoEm
) {
}
