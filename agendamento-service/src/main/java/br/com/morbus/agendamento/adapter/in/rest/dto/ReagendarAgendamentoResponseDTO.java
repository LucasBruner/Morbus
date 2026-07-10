package br.com.morbus.agendamento.adapter.in.rest.dto;

import br.com.morbus.agendamento.application.command.ReagendarAgendamentoResult;
import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReagendarAgendamentoResponseDTO(
        UUID id,
        EStatusAgendamento status,
        UUID slotId,
        LocalDateTime slotDate
) {

    public static ReagendarAgendamentoResponseDTO fromResult(ReagendarAgendamentoResult result) {
        return new ReagendarAgendamentoResponseDTO(
                result.id(),
                result.status(),
                result.slotId(),
                result.slotDate()
        );
    }
}
