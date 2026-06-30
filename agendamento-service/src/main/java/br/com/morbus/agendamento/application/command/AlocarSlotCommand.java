package br.com.morbus.agendamento.application.command;

import java.util.UUID;

public record AlocarSlotCommand(
        UUID queueEntryId,
        UUID slotId,
        UUID patientId
) {
}
