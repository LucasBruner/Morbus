package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.exception.SlotNotFoundException;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.IUnblockSlotUseCase;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UnblockSlotUseCase implements IUnblockSlotUseCase {

    private final ISlotRepository slotRepository;
    private final IScheduleRepository scheduleRepository;

    public UnblockSlotUseCase(ISlotRepository slotRepository, IScheduleRepository scheduleRepository) {
        this.slotRepository = slotRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public void execute(UUID id, UUID unitId) {
        Optional<Schedule> schedule = scheduleRepository.findById(id);

        if (!schedule.get().getUnitId().equals(unitId)) {
            throw new AccessDeniedException("EXECUTANTE restrito a sua unidade.");
        }

        List<Slot> slots = slotRepository
                .findByScheduleId(id)
                .stream()
                .filter(s -> s.getStatus().equals(EStatusSlots.INDISPONIVEL))
                .toList();

        if (slots.isEmpty()) {
            throw new SlotNotFoundException("Nao foram encontrados slots com esse scheduleId");
        }

        slots.forEach(Slot::unblock);
        slotRepository.saveAll(slots);
    }
}
