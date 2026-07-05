package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.adapter.out.rabbitmq.IAgendamentoEventPublisher;
import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtenderAgendamentoUseCaseTest {

    @Mock
    private IAgendamentoRepository agendamentoRepository;

    @Mock
    private ISlotRepository slotRepository;

    @Mock
    private IScheduleRepository scheduleRepository;

    @Mock
    private IAgendamentoEventPublisher eventPublisher;

    private AtenderAgendamentoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AtenderAgendamentoUseCase(agendamentoRepository, slotRepository, scheduleRepository, eventPublisher);
    }

    @Test
    void deveAtenderAgendamentoEManterSlotOcupado() {
        UUID appointmentId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID queueEntryId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        Agendamento agendamento = new Agendamento(
                appointmentId,
                queueEntryId,
                slotId,
                patientId,
                EStatusAgendamento.CONFIRMADO,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now(),
                null,
                null,
                LocalDateTime.now(),
                null
        );

        Slot slot = new Slot(slotId, scheduleId, LocalDateTime.now().plusHours(1), 1, 1, EStatusSlots.OCUPADO);
        Schedule schedule = new Schedule(unitId, UUID.randomUUID(), UUID.randomUUID(), null, LocalTime.NOON, LocalTime.MIDNIGHT, 30, 1);

        when(agendamentoRepository.findById(appointmentId)).thenReturn(Optional.of(agendamento));
        when(slotRepository.findById(slotId)).thenReturn(slot);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);
        when(slotRepository.save(slot)).thenReturn(slot);

        Agendamento result = useCase.execute(appointmentId, unitId);

        assertEquals(EStatusAgendamento.ATENDIDO, result.getStatus());
        assertNotNull(result.getAttendedAt());
        assertEquals(EStatusSlots.OCUPADO, slot.getStatus());
        verify(eventPublisher).publishAppointmentAttended(appointmentId, queueEntryId, patientId, result.getAttendedAt());
    }
}
