package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.adapter.out.rabbitmq.IAgendamentoEventPublisher;
import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.exception.AgendamentoNotFoundException;
import br.com.morbus.agendamento.domain.exception.CancelamentoNaoPermitidoException;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

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
class CancelarAgendamentoUseCaseTest {

    @Mock
    private IAgendamentoRepository agendamentoRepository;

    @Mock
    private ISlotRepository slotRepository;

    @Mock
    private IAgendamentoEventPublisher eventPublisher;

    private CancelarAgendamentoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CancelarAgendamentoUseCase(agendamentoRepository, slotRepository, eventPublisher);
    }

    private Agendamento buildAgendamento(UUID id, UUID slotId, UUID queueEntryId, UUID pacienteId,
                                         EStatusAgendamento status, LocalDateTime expiresAt) {
        return Agendamento.fromPersistence(new Agendamento.AgendamentoSnapshot(
                id, queueEntryId, slotId, pacienteId, status,
                expiresAt, null, null, null, null, LocalDateTime.now(), null));
    }

    @Test
    void deveCancelarAgendamentoLiberarSlotEPublicarEvento() {
        UUID agendamentoId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UUID queueEntryId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();

        Agendamento agendamento = buildAgendamento(agendamentoId, slotId, queueEntryId, pacienteId,
                EStatusAgendamento.CONFIRMADO, LocalDateTime.now().plusDays(1));
        Slot slot = new Slot(slotId, UUID.randomUUID(), LocalDateTime.now().plusHours(1), 1, 1, EStatusSlots.OCUPADO);

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(slotRepository.findById(slotId)).thenReturn(slot);
        when(slotRepository.save(slot)).thenReturn(slot);
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);

        useCase.execute(agendamentoId, pacienteId, "ROLE_PACIENTE", "Imprevisto");

        assertEquals(EStatusAgendamento.CANCELADO, agendamento.getStatus());
        assertEquals(EStatusSlots.DISPONIVEL, slot.getStatus());
        verify(eventPublisher).publishAppointmentCancelled(eq(agendamentoId), eq(queueEntryId), eq(pacienteId),
                eq("Imprevisto"), any());
    }

    @Test
    void deveCancelarQuandoRequisitanteEMedico() {
        UUID agendamentoId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        Agendamento agendamento = buildAgendamento(agendamentoId, slotId, UUID.randomUUID(), UUID.randomUUID(),
                EStatusAgendamento.AGUARDANDO_CONFIRMACAO, LocalDateTime.now().plusDays(1));
        Slot slot = new Slot(slotId, UUID.randomUUID(), LocalDateTime.now().plusHours(1), 1, 1, EStatusSlots.OCUPADO);

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(slotRepository.findById(slotId)).thenReturn(slot);
        when(slotRepository.save(slot)).thenReturn(slot);
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);

        useCase.execute(agendamentoId, UUID.randomUUID(), "ROLE_MEDICO", "Reorganizacao de agenda");

        assertEquals(EStatusAgendamento.CANCELADO, agendamento.getStatus());
    }

    @Test
    void deveLancarExcecaoQuandoAgendamentoNaoEncontrado() {
        UUID agendamentoId = UUID.randomUUID();
        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.empty());

        assertThrows(AgendamentoNotFoundException.class,
                () -> useCase.execute(agendamentoId, UUID.randomUUID(), "ROLE_PACIENTE", "motivo"));
    }

    @Test
    void deveLancarAccessDeniedQuandoPacienteNaoEDono() {
        UUID agendamentoId = UUID.randomUUID();
        Agendamento agendamento = buildAgendamento(agendamentoId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                EStatusAgendamento.CONFIRMADO, LocalDateTime.now().plusDays(1));

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(agendamentoId, UUID.randomUUID(), "ROLE_PACIENTE", "motivo"));

        verify(slotRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoStatusJaAtendidoOuFaltou() {
        UUID agendamentoId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        Agendamento agendamento = buildAgendamento(agendamentoId, UUID.randomUUID(), UUID.randomUUID(), pacienteId,
                EStatusAgendamento.ATENDIDO, LocalDateTime.now().plusDays(1));

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        assertThrows(CancelamentoNaoPermitidoException.class,
                () -> useCase.execute(agendamentoId, pacienteId, "ROLE_PACIENTE", "motivo"));

        verify(slotRepository, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoStatusNaoPermiteCancelamento() {
        UUID agendamentoId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        Agendamento agendamento = buildAgendamento(agendamentoId, UUID.randomUUID(), UUID.randomUUID(), pacienteId,
                EStatusAgendamento.CANCELADO, LocalDateTime.now().plusDays(1));

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        assertThrows(CancelamentoNaoPermitidoException.class,
                () -> useCase.execute(agendamentoId, pacienteId, "ROLE_PACIENTE", "motivo"));
    }

    @Test
    void deveLancarExcecaoQuandoJaPassouDaDataDoAtendimento() {
        UUID agendamentoId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        Agendamento agendamento = buildAgendamento(agendamentoId, UUID.randomUUID(), UUID.randomUUID(), pacienteId,
                EStatusAgendamento.CONFIRMADO, LocalDateTime.now().minusHours(1));

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        assertThrows(CancelamentoNaoPermitidoException.class,
                () -> useCase.execute(agendamentoId, pacienteId, "ROLE_PACIENTE", "motivo"));

        verify(slotRepository, never()).save(any());
    }
}
