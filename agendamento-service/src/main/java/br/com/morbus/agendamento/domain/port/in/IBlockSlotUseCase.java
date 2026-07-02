package br.com.morbus.agendamento.domain.port.in;

import br.com.morbus.agendamento.application.command.AlterarSlotStatusResult;

import java.util.UUID;

public interface IBlockSlotUseCase {

    AlterarSlotStatusResult execute(UUID id);
}
