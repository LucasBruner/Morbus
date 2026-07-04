package br.com.morbus.agendamento.domain.port.in;

import java.time.LocalDate;
import java.util.UUID;

public interface IBlockSlotUseCase {

    void execute(UUID id, UUID unitId, LocalDate date, String motivo);
}
