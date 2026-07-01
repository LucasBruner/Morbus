package br.com.morbus.agendamento.application.command;

import br.com.morbus.agendamento.domain.enums.EDiaSemana;

import java.time.LocalTime;
import java.util.UUID;

public record CriarScheduleCommand(
        UUID providerId,
        UUID unitId,
        UUID procedureId,
        EDiaSemana diaDaSemana,
        LocalTime horarioInicio,
        LocalTime horarioFim,
        Integer slotDuracaoMinutos,
        Integer capacidade
) {
}
