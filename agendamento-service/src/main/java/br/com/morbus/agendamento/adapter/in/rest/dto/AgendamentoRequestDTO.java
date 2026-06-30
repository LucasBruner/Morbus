package br.com.morbus.agendamento.adapter.in.rest.dto;

import br.com.morbus.agendamento.application.command.CriarAgendamentoCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AgendamentoRequestDTO(
        @NotNull(message = "O id da entrada na fila e obrigatorio") UUID queueEntryId,
        @NotNull(message = "O id do slot e obrigatorio") UUID slotId,
        @NotNull(message = "O id do paciente e obrigatorio") UUID pacienteId) {

    public CriarAgendamentoCommand toCommand() {
        return new CriarAgendamentoCommand(queueEntryId, slotId, pacienteId);
    }
}
