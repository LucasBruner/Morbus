package br.com.morbus.agendamento.adapter.in.rest.dto;

import br.com.morbus.agendamento.application.command.CriarScheduleCommand;
import br.com.morbus.agendamento.domain.enums.EDiaSemana;
import br.com.morbus.agendamento.domain.enums.ETurnos;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record ScheduleRequestDTO(
        @NotNull UUID providerId,
        @NotNull UUID unitId,
        @NotNull LocalDate dataInicio,
        @NotNull LocalDate dataFim,
        @NotNull ETurnos turno,
        @NotNull @Positive Integer slotDuracaoMinutos,
        @NotNull LocalTime horarioInicio,
        @NotNull LocalTime horarioFim,
        @NotEmpty List<EDiaSemana> diasDaSemana
) {

    public CriarScheduleCommand toCommand() {
        return new CriarScheduleCommand(
                providerId,
                unitId,
                dataInicio,
                dataFim,
                turno,
                slotDuracaoMinutos,
                horarioInicio,
                horarioFim,
                diasDaSemana
        );
    }
}
