package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.application.command.AlterarSlotStatusResult;
import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.exception.InvalidSlotStatusException;
import br.com.morbus.agendamento.domain.exception.SlotNotFoundException;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.IUnblockSlotUseCase;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;

import java.util.UUID;

public class UnblockSlotUseCase implements IUnblockSlotUseCase {

    private final ISlotRepository slotRepository;

    public UnblockSlotUseCase(ISlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @Override
    public AlterarSlotStatusResult execute(UUID id) {
        Slot slot = slotRepository.findById(id);

        if (slot == null) {
            throw new SlotNotFoundException("Nao foi encontrado um slot com esse id");
        }

        if (!slot.getStatus().equals(EStatusSlots.INDISPONIVEL)) {
            throw new InvalidSlotStatusException("Slot nao pode ser desbloqueado, verifique o status!");
        }

        slot.unblock();
        slotRepository.save(slot);

        return new AlterarSlotStatusResult(
                slot.getId(),
                slot.getScheduleId(),
                slot.getStatus());
    }
}
