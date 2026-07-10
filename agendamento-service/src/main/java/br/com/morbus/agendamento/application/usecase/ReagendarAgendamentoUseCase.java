package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.adapter.out.rabbitmq.IAgendamentoEventPublisher;
import br.com.morbus.agendamento.application.command.ReagendarAgendamentoResult;
import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.exception.AgendamentoNotFoundException;
import br.com.morbus.agendamento.domain.exception.InvalidAgendamentoStatusException;
import br.com.morbus.agendamento.domain.exception.SlotIndisponivelException;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.IReagendarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class ReagendarAgendamentoUseCase implements IReagendarAgendamentoUseCase {

    private final IAgendamentoRepository agendamentoRepository;
    private final ISlotRepository slotRepository;
    private final IAgendamentoEventPublisher eventPublisher;

    public ReagendarAgendamentoUseCase(IAgendamentoRepository agendamentoRepository,
                                       ISlotRepository slotRepository,
                                       IAgendamentoEventPublisher eventPublisher) {
        this.agendamentoRepository = agendamentoRepository;
        this.slotRepository = slotRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ReagendarAgendamentoResult execute(UUID agendamentoId, UUID newSlotId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new AgendamentoNotFoundException("Agendamento nao encontrado: " + agendamentoId));

        if (!(EStatusAgendamento.AGUARDANDO_CONFIRMACAO.equals(agendamento.getStatus())
                || EStatusAgendamento.CONFIRMADO.equals(agendamento.getStatus()))) {
            throw new InvalidAgendamentoStatusException("Status nao permite reagendamento");
        }

        Slot novoSlot = slotRepository.findById(newSlotId);

        if (!EStatusSlots.DISPONIVEL.equals(novoSlot.getStatus())) {
            throw new SlotIndisponivelException("Slot informado nao esta disponivel");
        }

        Slot slotAtual = slotRepository.findById(agendamento.getSlotId());
        slotAtual.releaseOne();
        slotRepository.save(slotAtual);

        novoSlot.reserveOne();
        Slot novoSlotSalvo = slotRepository.save(novoSlot);

        agendamento.reschedule(novoSlotSalvo.getId());
        Agendamento agendamentoSalvo = agendamentoRepository.save(agendamento);

        eventPublisher.publishAppointmentRescheduled(
                agendamentoSalvo.getId(),
                novoSlotSalvo.getId(),
                agendamentoSalvo.getQueueEntryId(),
                agendamentoSalvo.getPacienteId(),
                LocalDateTime.now(ZoneId.systemDefault()));

        return new ReagendarAgendamentoResult(
                agendamentoSalvo.getId(),
                agendamentoSalvo.getStatus(),
                novoSlotSalvo.getId(),
                novoSlotSalvo.getDataHora());
    }
}
