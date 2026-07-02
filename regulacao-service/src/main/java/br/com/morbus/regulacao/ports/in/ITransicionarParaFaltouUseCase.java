package br.com.morbus.regulacao.ports.in;

import br.com.morbus.regulacao.ports.in.dto.AppointmentNoShowCommand;

public interface ITransicionarParaFaltouUseCase {
    void execute(AppointmentNoShowCommand appointmentNoShowCommand);
}
