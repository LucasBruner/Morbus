package br.com.morbus.agendamento.application.command;

import java.time.LocalDateTime;
import java.util.UUID;

public record CriarAgendamentoCommand(
        UUID queueEntryId,
        UUID slotId,
        UUID pacienteId,
        LocalDateTime expiresAt
) {
}
