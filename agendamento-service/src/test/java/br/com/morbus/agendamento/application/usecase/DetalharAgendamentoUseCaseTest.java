package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.exception.AgendamentoNotFoundException;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.model.AgendamentoComDetalhes;
import br.com.morbus.agendamento.domain.model.HealthUnit;
import br.com.morbus.agendamento.domain.model.Provider;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import br.com.morbus.agendamento.domain.port.out.IHealthUnitRepository;
import br.com.morbus.agendamento.domain.port.out.IProviderRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetalharAgendamentoUseCaseTest {

    @Mock
    private IAgendamentoRepository agendamentoRepository;

    @Mock
    private ISlotRepository slotRepository;

    @Mock
    private IScheduleRepository scheduleRepository;

    @Mock
    private IHealthUnitRepository healthUnitRepository;

    @Mock
    private IProviderRepository providerRepository;

    private DetalharAgendamentoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DetalharAgendamentoUseCase(agendamentoRepository, slotRepository, scheduleRepository,
                healthUnitRepository, providerRepository);
    }

    private Agendamento buildAgendamento(UUID id, UUID slotId, UUID pacienteId) {
        return Agendamento.fromPersistence(new Agendamento.AgendamentoSnapshot(
                id, UUID.randomUUID(), slotId, pacienteId, EStatusAgendamento.CONFIRMADO,
                LocalDateTime.now().plusDays(1), null, null, null, null, LocalDateTime.now(), null));
    }

    @Test
    void deveRetornarDetalhesQuandoRequesterIdNulo() {
        UUID agendamentoId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();

        Agendamento agendamento = buildAgendamento(agendamentoId, slotId, UUID.randomUUID());
        Slot slot = new Slot(slotId, scheduleId, LocalDateTime.now().plusHours(1), 1, 0, EStatusSlots.DISPONIVEL);
        Schedule schedule = new Schedule(unitId, providerId, UUID.randomUUID(), null, LocalTime.NOON, LocalTime.MIDNIGHT, 30, 1);
        HealthUnit unit = new HealthUnit(unitId, "UBS Central", "1234567", "Sao Paulo", "SP");
        Provider provider = new Provider(providerId, "Dr. Joao", "12345-SP", "Clinico Geral");

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(slotRepository.findById(slotId)).thenReturn(slot);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(healthUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(providerRepository.findById(providerId)).thenReturn(Optional.of(provider));

        Optional<AgendamentoComDetalhes> result = useCase.execute(agendamentoId, null, null);

        assertTrue(result.isPresent());
        assertEquals(agendamento, result.get().agendamento());
        assertEquals(provider, result.get().provider());
    }

    @Test
    void deveRetornarDetalhesQuandoRequesterEMedico() {
        UUID agendamentoId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();

        Agendamento agendamento = buildAgendamento(agendamentoId, slotId, UUID.randomUUID());
        Slot slot = new Slot(slotId, scheduleId, LocalDateTime.now().plusHours(1), 1, 0, EStatusSlots.DISPONIVEL);
        Schedule schedule = new Schedule(unitId, null, UUID.randomUUID(), null, LocalTime.NOON, LocalTime.MIDNIGHT, 30, 1);
        HealthUnit unit = new HealthUnit(unitId, "UBS Central", "1234567", "Sao Paulo", "SP");

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));
        when(slotRepository.findById(slotId)).thenReturn(slot);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(healthUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));

        Optional<AgendamentoComDetalhes> result = useCase.execute(agendamentoId, UUID.randomUUID(), "ROLE_MEDICO");

        assertTrue(result.isPresent());
        assertNull(result.get().provider());
    }

    @Test
    void deveRetornarVazioQuandoPacienteNaoEDono() {
        UUID agendamentoId = UUID.randomUUID();
        Agendamento agendamento = buildAgendamento(agendamentoId, UUID.randomUUID(), UUID.randomUUID());

        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.of(agendamento));

        Optional<AgendamentoComDetalhes> result = useCase.execute(agendamentoId, UUID.randomUUID(), "ROLE_PACIENTE");

        assertTrue(result.isEmpty());
    }

    @Test
    void deveLancarExcecaoQuandoAgendamentoNaoEncontrado() {
        UUID agendamentoId = UUID.randomUUID();
        when(agendamentoRepository.findById(agendamentoId)).thenReturn(Optional.empty());

        assertThrows(AgendamentoNotFoundException.class, () -> useCase.execute(agendamentoId, UUID.randomUUID(), "ROLE_PACIENTE"));
    }
}
