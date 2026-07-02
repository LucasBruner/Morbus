package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.application.command.AlterarSlotStatusResult;
import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.exception.InvalidSlotStatusException;
import br.com.morbus.agendamento.domain.exception.SlotNotFoundException;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.IBlockSlotUseCase;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;

import java.util.UUID;

public class BlockSlotUseCase implements IBlockSlotUseCase {

    private final ISlotRepository slotRepository;

    public BlockSlotUseCase(ISlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @Override
    public AlterarSlotStatusResult execute(UUID id) {
        Slot slot = slotRepository.findById(id);

        if (slot == null) {
            throw new SlotNotFoundException("Nao foi encontrado um slot com esse id");
        }

        if (slot.getReservados() > 0 || !slot.getStatus().equals(EStatusSlots.DISPONIVEL)) {
            throw new InvalidSlotStatusException("Slot nao pode ser bloqueado, verifique o status!");
        }

        slot.block();
        slotRepository.save(slot);

        return new AlterarSlotStatusResult(
                slot.getId(),
                slot.getScheduleId(),
                slot.getStatus());
    }
}
