package br.com.morbus.agendamento.domain.port.in;

import java.util.UUID;

public interface IUnblockSlotUseCase {

    void execute(UUID id, UUID unitId);
}
