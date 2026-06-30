package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SlotPersistenceAdapter implements ISlotRepository {

    private final ISlotJpaRepository slotJpaRepository;

    public SlotPersistenceAdapter(ISlotJpaRepository slotJpaRepository) {
        this.slotJpaRepository = slotJpaRepository;
    }

    @Override
    public List<Slot> saveAll(List<Slot> slots) {
        return slotJpaRepository.saveAll(slots.stream()
                        .map(this::toEntity)
                        .toList())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private SlotEntity toEntity(Slot slot) {
        return new SlotEntity(
                slot.getId(),
                slot.getScheduleId(),
                slot.getDataHora(),
                slot.getCapacidade(),
                slot.getReservados(),
                slot.getStatus()
        );
    }

    private Slot toDomain(SlotEntity entity) {
        return new Slot(
                entity.getId(),
                entity.getScheduleId(),
                entity.getDataHora(),
                entity.getCapacidade(),
                entity.getReservados(),
                entity.getStatus()
        );
    }
}
