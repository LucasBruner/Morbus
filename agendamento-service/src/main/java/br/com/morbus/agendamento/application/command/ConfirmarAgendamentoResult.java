package br.com.morbus.agendamento.application.command;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConfirmarAgendamentoResult(
        UUID id,
        EStatusAgendamento status,
        LocalDateTime confirmedAt,
        LocalDateTime slotDate
) {
}
