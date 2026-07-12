package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.adapter.out.rabbitmq.IAgendamentoEventPublisher;
import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.exception.ScheduleNotFoundException;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpirarAgendamentosUseCaseTest {

    @Mock
    private IAgendamentoRepository agendamentoRepository;

    @Mock
    private ISlotRepository slotRepository;

    @Mock
    private IScheduleRepository scheduleRepository;

    @Mock
    private IAgendamentoEventPublisher eventPublisher;

    private ExpirarAgendamentosUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExpirarAgendamentosUseCase(agendamentoRepository, slotRepository, scheduleRepository, eventPublisher, 24L);
    }

    private Agendamento buildAgendamento(UUID id, UUID slotId, UUID queueEntryId, UUID pacienteId) {
        return Agendamento.fromPersistence(new Agendamento.AgendamentoSnapshot(
                id, queueEntryId, slotId, pacienteId, EStatusAgendamento.AGUARDANDO_CONFIRMACAO,
                LocalDateTime.now().minusHours(1), null, null, null, null, LocalDateTime.now(), null));
    }

    @Test
    void deveEncontrarAgendamentosExpirados() {
        Agendamento agendamento = buildAgendamento(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(agendamentoRepository.findAllByStatusAndExpiresAtBefore(eq(EStatusAgendamento.AGUARDANDO_CONFIRMACAO), any()))
                .thenReturn(List.of(agendamento));

        List<Agendamento> result = useCase.findExpiredAppointments();

        assertEquals(1, result.size());
        assertTrue(result.contains(agendamento));
    }

    @Test
    void deveExpirarAgendamentoLiberarSlotEPublicarEvento() {
        UUID agendamentoId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID queueEntryId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();

        Agendamento agendamento = buildAgendamento(agendamentoId, slotId, queueEntryId, pacienteId);
        Slot slot = new Slot(slotId, scheduleId, LocalDateTime.now().plusHours(1), 1, 1, EStatusSlots.OCUPADO);
        Schedule schedule = new Schedule(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, LocalTime.NOON, LocalTime.MIDNIGHT, 30, 1);

        when(slotRepository.findById(slotId)).thenReturn(slot);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(slotRepository.save(slot)).thenReturn(slot);
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);

        Agendamento result = useCase.expireAppointment(agendamento);

        assertEquals(EStatusAgendamento.CANCELADO, result.getStatus());
        assertEquals("EXPIRACAO_24H", result.getCancellationReason());
        assertEquals(EStatusSlots.DISPONIVEL, slot.getStatus());
        verify(eventPublisher).publishAppointmentExpired(eq(agendamentoId), eq(queueEntryId), eq(pacienteId), any());
    }

    @Test
    void deveLancarExcecaoQuandoScheduleNaoEncontradoParaOSlot() {
        UUID slotId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        Agendamento agendamento = buildAgendamento(UUID.randomUUID(), slotId, UUID.randomUUID(), UUID.randomUUID());
        Slot slot = new Slot(slotId, scheduleId, LocalDateTime.now().plusHours(1), 1, 1, EStatusSlots.OCUPADO);

        when(slotRepository.findById(slotId)).thenReturn(slot);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        assertThrows(ScheduleNotFoundException.class, () -> useCase.expireAppointment(agendamento));
    }
}
