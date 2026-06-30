package br.com.morbus.agendamento.application.command;

import br.com.morbus.agendamento.domain.model.Schedule;

public record CriarScheduleResult(Schedule schedule, int slotsGerados) {
}
