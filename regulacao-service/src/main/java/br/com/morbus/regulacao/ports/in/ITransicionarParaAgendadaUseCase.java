package br.com.morbus.regulacao.ports.in;

import br.com.morbus.regulacao.ports.in.dto.AppointmentCreatedCommand;

public interface ITransicionarParaAgendadaUseCase {
    void execute(AppointmentCreatedCommand appointmentCreatedCommand);
}
