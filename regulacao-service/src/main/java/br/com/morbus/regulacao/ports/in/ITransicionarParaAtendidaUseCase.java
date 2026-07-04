package br.com.morbus.regulacao.ports.in;

import br.com.morbus.regulacao.ports.in.dto.AppointmentAttendedCommand;

public interface ITransicionarParaAtendidaUseCase {
    void execute(AppointmentAttendedCommand appointmentAttendedCommand);
}
