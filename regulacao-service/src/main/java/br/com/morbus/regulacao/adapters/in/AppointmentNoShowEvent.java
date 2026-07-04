package br.com.morbus.regulacao.adapters.in;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentNoShowEvent(UUID solicitacaoId,
                                     UUID appointmentId,
                                     LocalDateTime ocorridoEm) {
}
