package br.com.morbus.agendamento.domain.port.in;

import br.com.morbus.agendamento.domain.model.Agendamento;

import java.util.UUID;

public interface IRegistrarFaltaAgendamentoUseCase {

    Agendamento execute(UUID agendamentoId, UUID unitId);
}
