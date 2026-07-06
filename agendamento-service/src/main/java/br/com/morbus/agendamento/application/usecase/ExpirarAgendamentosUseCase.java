package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.adapter.out.rabbitmq.IAgendamentoEventPublisher;
import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.exception.ScheduleNotFoundException;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public class ExpirarAgendamentosUseCase {

    private final IAgendamentoRepository agendamentoRepository;
    private final ISlotRepository slotRepository;
    private final IScheduleRepository scheduleRepository;
    private final IAgendamentoEventPublisher eventPublisher;

    public ExpirarAgendamentosUseCase(IAgendamentoRepository agendamentoRepository,
                                      ISlotRepository slotRepository,
                                      IScheduleRepository scheduleRepository,
                                      IAgendamentoEventPublisher eventPublisher) {
        this.agendamentoRepository = agendamentoRepository;
        this.slotRepository = slotRepository;
        this.scheduleRepository = scheduleRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<Agendamento> findExpiredAppointments() {
        return agendamentoRepository.findAllByStatusAndExpiresAtBefore(
                EStatusAgendamento.AGUARDANDO_CONFIRMACAO,
                LocalDateTime.now()
        );
    }

    @Transactional
    public Agendamento expireAppointment(Agendamento agendamento) {
        Slot slot = slotRepository.findById(agendamento.getSlotId());
        scheduleRepository.findById(slot.getScheduleId())
                .orElseThrow(() -> new ScheduleNotFoundException("Schedule nao encontrado para o slot"));

        agendamento.cancel("EXPIRACAO_72H");
        slot.releaseOne();

        slotRepository.save(slot);
        Agendamento saved = agendamentoRepository.save(agendamento);

        eventPublisher.publishAppointmentExpired(
                saved.getId(),
                saved.getQueueEntryId(),
                saved.getPacienteId(),
                LocalDateTime.now()
        );

        return saved;
    }
}
