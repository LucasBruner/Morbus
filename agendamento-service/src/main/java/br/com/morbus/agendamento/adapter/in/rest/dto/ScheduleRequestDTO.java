package br.com.morbus.agendamento.adapter.in.rest.dto;

import br.com.morbus.agendamento.application.command.CriarScheduleCommand;
import br.com.morbus.agendamento.domain.enums.EDiaSemana;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalTime;
import java.util.UUID;

public record ScheduleRequestDTO(
        @NotNull UUID providerId,
        @NotNull UUID unitId,
        @NotNull UUID procedureId,
        @NotNull EDiaSemana diaDaSemana,
        @NotNull LocalTime horarioInicio,
        @NotNull LocalTime horarioFim,
        @NotNull @Positive Integer slotDuracaoMinutos,
        @NotNull @Positive Integer capacidade
) {

    public CriarScheduleCommand toCommand() {
        return new CriarScheduleCommand(
                providerId,
                unitId,
                procedureId,
                diaDaSemana,
                horarioInicio,
                horarioFim,
                slotDuracaoMinutos,
                capacidade
        );
    }
}
