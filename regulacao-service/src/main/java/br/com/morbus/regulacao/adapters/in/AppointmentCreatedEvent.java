package br.com.morbus.regulacao.adapters.in;

import java.util.UUID;

public record AppointmentCreatedEvent(UUID solicitacaoId, UUID appointmentId, UUID slotId) {
}
