package br.com.morbus.agendamento.adapter.out.rabbitmq;

import java.time.LocalDateTime;
import java.util.UUID;

public record PatientNoShowEvent(
        UUID appointmentId,
        UUID queueEntryId,
        UUID patientId,
        LocalDateTime ocorridoEm
) {
}
