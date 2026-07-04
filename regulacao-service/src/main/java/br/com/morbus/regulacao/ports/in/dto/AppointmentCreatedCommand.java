package br.com.morbus.regulacao.ports.in.dto;

import java.util.UUID;

public record AppointmentCreatedCommand(UUID solicitacaoId,
                                        UUID appointmentId,
                                        UUID slotId) {
}
