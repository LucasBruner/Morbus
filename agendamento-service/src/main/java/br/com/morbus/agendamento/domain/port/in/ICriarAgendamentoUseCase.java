package br.com.morbus.agendamento.domain.port.in;

import br.com.morbus.agendamento.application.command.CriarAgendamentoCommand;
import br.com.morbus.agendamento.domain.model.Agendamento;

public interface ICriarAgendamentoUseCase {

    Agendamento execute(CriarAgendamentoCommand command);
}
