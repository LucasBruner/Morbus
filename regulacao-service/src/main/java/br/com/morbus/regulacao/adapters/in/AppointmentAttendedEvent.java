package br.com.morbus.regulacao.adapters.in;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentAttendedEvent(UUID solicitacaoId,
                                       UUID appointmentId,
                                       LocalDateTime ocorridoEm) {
}
