package br.com.morbus.agendamento.domain.port.in;

import java.util.UUID;

public interface ICancelarAgendamentoUseCase {

    void execute(UUID agendamentoId, UUID requesterId, String requesterRole, String motivo);
}
