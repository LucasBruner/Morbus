package br.com.morbus.agendamento.domain.port.in;

import br.com.morbus.agendamento.adapter.in.rabbitmq.PatientCalledEvent;
import br.com.morbus.agendamento.domain.model.Agendamento;

import java.util.Optional;

public interface IAlocarPacienteEmSlotUseCase {

    Optional<Agendamento> execute(PatientCalledEvent event);
}
