package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.adapter.in.rabbitmq.PatientCalledEvent;
import br.com.morbus.agendamento.adapter.out.rabbitmq.IAgendamentoEventPublisher;
import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.IAlocarPacienteEmSlotUseCase;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

public class AlocarPacienteEmSlotUseCase implements IAlocarPacienteEmSlotUseCase {

    private final IAgendamentoRepository agendamentoRepository;
    private final ISlotRepository slotRepository;
    private final IAgendamentoEventPublisher eventPublisher;

    public AlocarPacienteEmSlotUseCase(IAgendamentoRepository agendamentoRepository,
                                       ISlotRepository slotRepository,
                                       IAgendamentoEventPublisher eventPublisher) {
        this.agendamentoRepository = agendamentoRepository;
        this.slotRepository = slotRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Optional<Agendamento> execute(PatientCalledEvent event) {
        Optional<Slot> slotOpt = slotRepository.findAvailableSlotForProcedureAndUnit(
                event.procedureId(),
                event.preferredUnitId()
        );

        if (slotOpt.isEmpty()) {
            eventPublisher.publishAppointmentNoSlot(event.queueEntryId(), event.pacienteId(), event.procedureId());
            return Optional.empty();
        }

        Slot slot = slotOpt.get();
        if (!EStatusSlots.DISPONIVEL.equals(slot.getStatus())) {
            eventPublisher.publishAppointmentNoSlot(event.queueEntryId(), event.pacienteId(), event.procedureId());
            return Optional.empty();
        }

        slot.reserveOne();
        Slot savedSlot = slotRepository.save(slot);

        Agendamento agendamento = new Agendamento(
                event.queueEntryId(),
                savedSlot.getId(),
                event.pacienteId(),
                LocalDateTime.now(ZoneId.systemDefault()).plusHours(72)
        );

        Agendamento savedAgendamento = agendamentoRepository.save(agendamento);
        eventPublisher.publishAppointmentConfirmed(
                savedAgendamento.getId(),
                savedSlot.getId(),
                event.queueEntryId(),
                event.pacienteId(),
                savedAgendamento.getCreatedAt()
        );

        return Optional.of(savedAgendamento);
    }
}
