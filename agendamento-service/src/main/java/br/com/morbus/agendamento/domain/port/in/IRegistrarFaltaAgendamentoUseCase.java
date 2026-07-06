package br.com.morbus.agendamento.domain.port.in;

import java.util.UUID;

public interface IRegistrarFaltaAgendamentoUseCase {

    void execute(UUID agendamentoId, UUID unitId);
}
