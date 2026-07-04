package br.com.morbus.agendamento.domain.port.out;

import br.com.morbus.agendamento.domain.model.Slot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ISlotRepository {

    List<Slot> saveAll(List<Slot> slots);

    Slot save(Slot slot);

    Slot findById(UUID id);

    List<Slot> findByScheduleId(UUID id);

    Optional<Slot> findAvailableSlotForProcedureAndUnit(UUID procedureId, UUID preferredUnitId);
}
