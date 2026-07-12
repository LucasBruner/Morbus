package br.com.morbus.agendamento.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReagendarAgendamentoRequestDTO(
        @NotNull UUID newSlotId
) {
}
