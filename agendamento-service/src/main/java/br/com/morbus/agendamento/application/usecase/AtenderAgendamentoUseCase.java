package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.adapter.out.rabbitmq.IAgendamentoEventPublisher;
import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.exception.AgendamentoNotFoundException;
import br.com.morbus.agendamento.domain.exception.InvalidAgendamentoStatusException;
import br.com.morbus.agendamento.domain.exception.ScheduleNotFoundException;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.IAtenderAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class AtenderAgendamentoUseCase implements IAtenderAgendamentoUseCase {

    private final IAgendamentoRepository agendamentoRepository;
    private final ISlotRepository slotRepository;
    private final IScheduleRepository scheduleRepository;
    private final IAgendamentoEventPublisher eventPublisher;

    public AtenderAgendamentoUseCase(IAgendamentoRepository agendamentoRepository,
                                     ISlotRepository slotRepository,
                                     IScheduleRepository scheduleRepository,
                                     IAgendamentoEventPublisher eventPublisher) {
        this.agendamentoRepository = agendamentoRepository;
        this.slotRepository = slotRepository;
        this.scheduleRepository = scheduleRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Agendamento execute(UUID agendamentoId, UUID unitId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new AgendamentoNotFoundException("Agendamento nao encontrado: " + agendamentoId));

        if (!EStatusAgendamento.CONFIRMADO.equals(agendamento.getStatus())) {
            throw new InvalidAgendamentoStatusException("Agendamento deve estar confirmado para atendimento");
        }

        Slot slot = slotRepository.findById(agendamento.getSlotId());
        Schedule schedule = scheduleRepository.findById(slot.getScheduleId())
                .orElseThrow(() -> new ScheduleNotFoundException("Schedule nao encontrado para o slot"));

        if (!schedule.getUnitId().equals(unitId)) {
            throw new AccessDeniedException("EXECUTANTE restrito a sua unidade.");
        }

        agendamento.attend();

        slotRepository.save(slot);
        Agendamento saved = agendamentoRepository.save(agendamento);

        eventPublisher.publishAppointmentAttended(
                saved.getId(),
                saved.getQueueEntryId(),
                saved.getPacienteId(),
                saved.getAttendedAt()
        );

        return saved;
    }
}
