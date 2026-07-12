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
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReagendarAgendamentoUseCaseTest {

    @Mock
    private IAgendamentoRepository agendamentoRepository;

    @Mock
    private ISlotRepository slotRepository;

    @Mock
    private IAgendamentoEventPublisher eventPublisher;

    private ReagendarAgendamentoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReagendarAgendamentoUseCase(agendamentoRepository, slotRepository, eventPublisher);
    }

    private Agendamento buildAgendamento(UUID id, UUID slotId, UUID queueEntryId, UUID patientId, EStatusAgendamento status) {
        return Agendamento.fromPersistence(new Agendamento.AgendamentoSnapshot(
                id, queueEntryId, slotId, patientId, status,
                LocalDateTime.now().plusDays(1), null, null, null, null,
                LocalDateTime.now(), null));
    }

    @Test
    void deveReagendarParaNovoSlotDisponivel() {
        UUID appointmentId = UUID.randomUUID();
        UUID slotAtualId = UUID.randomUUID();
        UUID novoSlotId = UUID.randomUUID();
        UUID queueEntryId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        Agendamento agendamento = buildAgendamento(appointmentId, slotAtualId, queueEntryId, patientId,
                EStatusAgendamento.CONFIRMADO);
        Slot slotAtual = new Slot(slotAtualId, UUID.randomUUID(), LocalDateTime.now().plusHours(1), 1, 1, EStatusSlots.OCUPADO);
        Slot novoSlot = new Slot(novoSlotId, UUID.randomUUID(), LocalDateTime.now().plusHours(2), 1, 0, EStatusSlots.DISPONIVEL);

        when(agendamentoRepository.findById(appointmentId)).thenReturn(Optional.of(agendamento));
        when(slotRepository.findById(novoSlotId)).thenReturn(novoSlot);
        when(slotRepository.findById(slotAtualId)).thenReturn(slotAtual);
        when(slotRepository.save(slotAtual)).thenReturn(slotAtual);
        when(slotRepository.save(novoSlot)).thenReturn(novoSlot);
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);

        ReagendarAgendamentoResult result = useCase.execute(appointmentId, novoSlotId);

        assertEquals(novoSlotId, result.slotId());
        assertEquals(EStatusSlots.DISPONIVEL, slotAtual.getStatus());
        assertEquals(EStatusSlots.OCUPADO, novoSlot.getStatus());
        verify(eventPublisher).publishAppointmentRescheduled(eq(appointmentId), eq(novoSlotId), eq(queueEntryId), eq(patientId), any());
    }

    @Test
    void deveLancarExcecaoQuandoAgendamentoNaoExiste() {
        UUID appointmentId = UUID.randomUUID();
        when(agendamentoRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThrows(AgendamentoNotFoundException.class,
                () -> useCase.execute(appointmentId, UUID.randomUUID()));
    }

    @Test
    void deveLancarExcecaoQuandoStatusNaoPermiteReagendamento() {
        UUID appointmentId = UUID.randomUUID();
        Agendamento agendamento = buildAgendamento(appointmentId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), EStatusAgendamento.CANCELADO);

        when(agendamentoRepository.findById(appointmentId)).thenReturn(Optional.of(agendamento));

        assertThrows(InvalidAgendamentoStatusException.class,
                () -> useCase.execute(appointmentId, UUID.randomUUID()));

        verify(slotRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoNovoSlotNaoEstaDisponivel() {
        UUID appointmentId = UUID.randomUUID();
        UUID novoSlotId = UUID.randomUUID();
        Agendamento agendamento = buildAgendamento(appointmentId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), EStatusAgendamento.CONFIRMADO);
        Slot novoSlot = new Slot(novoSlotId, UUID.randomUUID(), LocalDateTime.now().plusHours(2), 1, 1, EStatusSlots.OCUPADO);

        when(agendamentoRepository.findById(appointmentId)).thenReturn(Optional.of(agendamento));
        when(slotRepository.findById(novoSlotId)).thenReturn(novoSlot);

        assertThrows(SlotIndisponivelException.class, () -> useCase.execute(appointmentId, novoSlotId));

        verify(agendamentoRepository, never()).save(any());
    }
}
