package br.com.morbus.agendamento.domain.port.in;

import br.com.morbus.agendamento.application.command.ConfirmarAgendamentoResult;

import java.util.UUID;

public interface IConfirmarAgendamentoUseCase {

    ConfirmarAgendamentoResult execute(UUID agendamentoId, UUID userId);
}
