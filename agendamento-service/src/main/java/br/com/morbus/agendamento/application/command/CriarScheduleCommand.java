package br.com.morbus.agendamento.application.command;

import br.com.morbus.agendamento.domain.enums.EDiaSemana;
import br.com.morbus.agendamento.domain.enums.ETurnos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CriarScheduleCommand(
        UUID providerId,
        UUID unitId,
        LocalDate dataInicio,
        LocalDate dataFim,
        ETurnos turno,
        Integer slotDuracaoMinutos,
        LocalTime horarioInicio,
        LocalTime horarioFim,
        List<EDiaSemana> diasDaSemana
) {
}
