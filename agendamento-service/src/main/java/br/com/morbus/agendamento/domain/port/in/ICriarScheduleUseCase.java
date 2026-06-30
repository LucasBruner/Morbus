package br.com.morbus.agendamento.domain.port.in;

import br.com.morbus.agendamento.application.command.CriarScheduleCommand;
import br.com.morbus.agendamento.application.command.CriarScheduleResult;

public interface ICriarScheduleUseCase {

    CriarScheduleResult execute(CriarScheduleCommand command);
}
