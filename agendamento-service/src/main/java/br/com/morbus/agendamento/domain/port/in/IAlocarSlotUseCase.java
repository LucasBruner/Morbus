package br.com.morbus.agendamento.domain.port.in;

import br.com.morbus.agendamento.application.command.AlocarSlotCommand;
import br.com.morbus.agendamento.domain.model.Appointment;

public interface IAlocarSlotUseCase {

    Appointment execute(AlocarSlotCommand command);
}
