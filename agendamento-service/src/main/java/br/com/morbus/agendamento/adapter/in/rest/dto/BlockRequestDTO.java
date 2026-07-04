package br.com.morbus.agendamento.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BlockRequestDTO(
        @NotNull LocalDate date,
        @NotNull String motivo
) {}
