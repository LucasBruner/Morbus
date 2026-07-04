package br.com.morbus.regulacao.ports.in.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentNoShowCommand(UUID solicitacaoId,
                                       UUID appointmentId,
                                       LocalDateTime ocorridoEm) {
}
