package br.com.morbus.agendamento.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalTime;
import java.util.UUID;

public record AtualizarScheduleRequestDTO(
        @NotNull UUID providerId,
        @NotNull LocalTime horarioInicio,
        @NotNull LocalTime horarioFim,
        @NotNull @Positive Integer capacidade
) {
}
