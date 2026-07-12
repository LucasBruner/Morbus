package br.com.morbus.agendamento.adapter.out.rabbitmq;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentRescheduledEvent(
        UUID appointmentId,
        UUID slotId,
        UUID queueEntryId,
        UUID patientId,
        LocalDateTime reagendadoEm
) {
}
