package br.com.morbus.agendamento.application.command;

import br.com.morbus.agendamento.domain.enums.EStatusSlots;

import java.util.UUID;

public record AlterarSlotStatusResult(
                UUID slotId,
                UUID scheduleId,
                EStatusSlots status) {
}
