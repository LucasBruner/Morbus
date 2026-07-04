package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.exception.ScheduleNotFoundException;
import br.com.morbus.agendamento.domain.exception.SlotNotFoundException;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.IBlockSlotUseCase;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class BlockSlotUseCase implements IBlockSlotUseCase {

    private final ISlotRepository slotRepository;
    private final IScheduleRepository scheduleRepository;

    public BlockSlotUseCase(ISlotRepository slotRepository, IScheduleRepository scheduleRepository) {
        this.slotRepository = slotRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public void execute(UUID id, UUID unitId, LocalDate date, String motivo) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ScheduleNotFoundException("Grade nao encontrada: " + id));

        if (!schedule.getUnitId().equals(unitId)) {
            throw new AccessDeniedException("EXECUTANTE restrito a sua unidade.");
        }

        List<Slot> slots = slotRepository
                .findByScheduleId(id)
                .stream()
                .filter(s ->
                        s.getStatus().equals(EStatusSlots.DISPONIVEL) &&
                        s.getDataHora().toLocalDate().equals(date))
                .toList();

        if (slots.isEmpty()) {
            throw new SlotNotFoundException("Nao foram encontrados slots disponiveis com esse scheduleId e date");
        }

        slots.forEach(Slot::block);
        slotRepository.saveAll(slots);
    }
}
