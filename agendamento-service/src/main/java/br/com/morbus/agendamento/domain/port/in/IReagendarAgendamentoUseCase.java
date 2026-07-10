package br.com.morbus.agendamento.domain.port.in;

import br.com.morbus.agendamento.application.command.ReagendarAgendamentoResult;

import java.util.UUID;

public interface IReagendarAgendamentoUseCase {

    ReagendarAgendamentoResult execute(UUID agendamentoId, UUID newSlotId);
}
