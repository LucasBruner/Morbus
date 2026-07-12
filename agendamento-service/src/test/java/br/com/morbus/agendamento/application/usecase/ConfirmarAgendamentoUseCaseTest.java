package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.application.command.ConfirmarAgendamentoResult;
import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.exception.AgendamentoNotFoundException;
import br.com.morbus.agendamento.domain.exception.ExpiredConfirmationException;
import br.com.morbus.agendamento.domain.exception.InvalidAgendamentoStatusException;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.model.HealthUnit;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import br.com.morbus.agendamento.domain.port.out.IHealthUnitRepository;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmarAgendamentoUseCaseTest {

    @Mock
    private IAgendamentoRepository agendamentoRepository;

    @Mock
    private ISlotRepository slotRepository;

    @Mock
    private IScheduleRepository scheduleRepository;

    @Mock
    private IHealthUnitRepository healthUnitRepository;

    private ConfirmarAgendamentoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConfirmarAgendamentoUseCase(agendamentoRepository, slotRepository, scheduleRepository, healthUnitRepository);
    }

    private Agendamento buildAgendamento(UUID id, UUID slotId, UUID pacienteId, EStatusAgendamento status, LocalDateTime expiresAt) {
        return Agendamento.fromPersistence(new Agendamento.AgendamentoSnapshot(
                id, UUID.randomUUID(), slotId, pacienteId, status,
                expiresAt, null, null, null, null, LocalDateTime.now(), null));
    }

    @Test
    void deveConfirmarAgendamentoComDadosDaUnidade() {
        UUID agendamentoId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();

        Agendamento agendamento = buildAgendamento(agendamentoId, slotId, pacienteId,
                EStatusAgendamento.AGUARDANDO_CONFIRMACAO, LocalDateTime.now().plusHours(1));
        Slot slot = new Slot(slotId, scheduleId, LocalDateTime.now().plusHours(2), 1, 0, EStatusSlots.DISPONIVEL);
        Schedule schedule = new Schedule(unitId, UUID.randomUUID(), UUID.randomUUID(), null, LocalTime.NOON, LocalTime.MIDNIGHT, 30, 1);
        HealthUnit healthUnit = new HealthUnit(unitId, "UBS Central", "1234567", "Sao Paulo", "SP", "Rua A, 100");

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);
        when(slotRepository.findById(slotId)).thenReturn(slot);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(healthUnitRepository.findById(unitId)).thenReturn(Optional.of(healthUnit));

        ConfirmarAgendamentoResult result = useCase.execute(agendamentoId, pacienteId);

        assertEquals(agendamentoId, result.id());
        assertEquals(EStatusAgendamento.CONFIRMADO, result.status());
        assertEquals(slot.getDataHora(), result.slotDate());
        assertEquals("UBS Central", result.unitName());
        assertEquals("Rua A, 100", result.unitAddress());
        assertNotNull(agendamento.getConfirmedAt());
    }

    @Test
    void deveConfirmarSemDadosDeUnidadeQuandoScheduleNaoEncontrada() {
        UUID agendamentoId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        Agendamento agendamento = buildAgendamento(agendamentoId, slotId, pacienteId,
                EStatusAgendamento.AGUARDANDO_CONFIRMACAO, LocalDateTime.now().plusHours(1));
        Slot slot = new Slot(slotId, scheduleId, LocalDateTime.now().plusHours(2), 1, 0, EStatusSlots.DISPONIVEL);

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(agendamento)).thenReturn(agendamento);
        when(slotRepository.findById(slotId)).thenReturn(slot);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        ConfirmarAgendamentoResult result = useCase.execute(agendamentoId, pacienteId);

        assertNull(result.unitName());
        assertNull(result.unitAddress());
    }

    @Test
    void deveLancarExcecaoQuandoAgendamentoNaoEncontrado() {
        UUID agendamentoId = UUID.randomUUID();
        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.empty());

        assertThrows(AgendamentoNotFoundException.class, () -> useCase.execute(agendamentoId, UUID.randomUUID()));
    }

    @Test
    void deveLancarAccessDeniedQuandoUsuarioNaoEDono() {
        UUID agendamentoId = UUID.randomUUID();
        Agendamento agendamento = buildAgendamento(agendamentoId, UUID.randomUUID(), UUID.randomUUID(),
                EStatusAgendamento.AGUARDANDO_CONFIRMACAO, LocalDateTime.now().plusHours(1));

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        assertThrows(AccessDeniedException.class, () -> useCase.execute(agendamentoId, UUID.randomUUID()));
    }

    @Test
    void deveLancarExcecaoQuandoStatusNaoEAguardandoConfirmacao() {
        UUID agendamentoId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        Agendamento agendamento = buildAgendamento(agendamentoId, UUID.randomUUID(), pacienteId,
                EStatusAgendamento.CONFIRMADO, LocalDateTime.now().plusHours(1));

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        assertThrows(InvalidAgendamentoStatusException.class, () -> useCase.execute(agendamentoId, pacienteId));
    }

    @Test
    void deveLancarExcecaoQuandoPrazoDeConfirmacaoExpirado() {
        UUID agendamentoId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        Agendamento agendamento = buildAgendamento(agendamentoId, UUID.randomUUID(), pacienteId,
                EStatusAgendamento.AGUARDANDO_CONFIRMACAO, LocalDateTime.now().minusHours(1));

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        assertThrows(ExpiredConfirmationException.class, () -> useCase.execute(agendamentoId, pacienteId));
    }
}
