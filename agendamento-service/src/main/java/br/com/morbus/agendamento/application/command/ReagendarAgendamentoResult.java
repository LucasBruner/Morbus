package br.com.morbus.agendamento.application.command;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReagendarAgendamentoResult(
        UUID id,
        EStatusAgendamento status,
        UUID slotId,
        LocalDateTime slotDate
) {
}
