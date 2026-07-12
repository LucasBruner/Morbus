package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.adapter.in.rabbitmq.PatientCalledEvent;
import br.com.morbus.agendamento.adapter.out.rabbitmq.IAgendamentoEventPublisher;
import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlocarPacienteEmSlotUseCaseTest {

    @Mock
    private IAgendamentoRepository agendamentoRepository;

    @Mock
    private ISlotRepository slotRepository;

    @Mock
    private IAgendamentoEventPublisher eventPublisher;

    private AlocarPacienteEmSlotUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AlocarPacienteEmSlotUseCase(agendamentoRepository, slotRepository, eventPublisher, 24L);
    }

    private PatientCalledEvent buildEvent(UUID queueEntryId, UUID patientId, UUID procedureId, UUID unitId, UUID solicitacaoId) {
        return new PatientCalledEvent("PATIENT_CALLED", queueEntryId, patientId, null, "Paciente Teste",
                "11999999999", "Consulta", procedureId, unitId, solicitacaoId, "AZUL", "FILA_REGULADA", Instant.now());
    }

    @Test
    void deveAlocarPacienteQuandoHouverSlotDisponivel() {
        UUID queueEntryId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID procedureId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID solicitacaoId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        Slot slot = new Slot(slotId, UUID.randomUUID(), LocalDateTime.now().plusHours(3), 1, 0, EStatusSlots.DISPONIVEL);
        PatientCalledEvent event = buildEvent(queueEntryId, patientId, procedureId, unitId, solicitacaoId);

        when(slotRepository.findAvailableSlotForProcedureAndUnit(procedureId, unitId)).thenReturn(Optional.of(slot));
        when(slotRepository.save(slot)).thenReturn(slot);
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Agendamento> result = useCase.execute(event);

        assertTrue(result.isPresent());
        assertEquals(patientId, result.get().getPacienteId());
        assertEquals(slotId, result.get().getSlotId());
        assertEquals(EStatusSlots.OCUPADO, slot.getStatus());
        verify(eventPublisher).publishAppointmentConfirmed(eq(result.get().getId()), eq(slotId), eq(queueEntryId), eq(patientId), any());
        verify(eventPublisher).publishAppointmentCreated(eq(solicitacaoId), eq(result.get().getId()), eq(slotId));
    }

    @Test
    void naoDevePublicarCreatedQuandoSolicitacaoIdForNulo() {
        UUID queueEntryId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID procedureId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        Slot slot = new Slot(slotId, UUID.randomUUID(), LocalDateTime.now().plusHours(3), 1, 0, EStatusSlots.DISPONIVEL);
        PatientCalledEvent event = buildEvent(queueEntryId, patientId, procedureId, unitId, null);

        when(slotRepository.findAvailableSlotForProcedureAndUnit(procedureId, unitId)).thenReturn(Optional.of(slot));
        when(slotRepository.save(slot)).thenReturn(slot);
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(event);

        verify(eventPublisher, never()).publishAppointmentCreated(any(), any(), any());
    }

    @Test
    void deveRetornarVazioEPublicarNoSlotQuandoNaoHouverSlotDisponivel() {
        UUID queueEntryId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID procedureId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();

        PatientCalledEvent event = buildEvent(queueEntryId, patientId, procedureId, unitId, null);

        when(slotRepository.findAvailableSlotForProcedureAndUnit(procedureId, unitId)).thenReturn(Optional.empty());

        Optional<Agendamento> result = useCase.execute(event);

        assertTrue(result.isEmpty());
        verify(eventPublisher).publishAppointmentNoSlot(queueEntryId, patientId, procedureId);
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    void deveRetornarVazioQuandoSlotEncontradoNaoEstiverDisponivel() {
        UUID queueEntryId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID procedureId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();

        Slot slot = new Slot(UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().plusHours(3), 1, 1, EStatusSlots.OCUPADO);
        PatientCalledEvent event = buildEvent(queueEntryId, patientId, procedureId, unitId, null);

        when(slotRepository.findAvailableSlotForProcedureAndUnit(procedureId, unitId)).thenReturn(Optional.of(slot));

        Optional<Agendamento> result = useCase.execute(event);

        assertTrue(result.isEmpty());
        verify(eventPublisher).publishAppointmentNoSlot(queueEntryId, patientId, procedureId);
        verify(agendamentoRepository, never()).save(any());
    }
}
