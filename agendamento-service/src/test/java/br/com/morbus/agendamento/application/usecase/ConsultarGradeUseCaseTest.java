package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.enums.EDiaSemana;
import br.com.morbus.agendamento.domain.model.HealthUnit;
import br.com.morbus.agendamento.domain.model.Provider;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.port.in.IConsultarGradeUseCase.GradeItem;
import br.com.morbus.agendamento.domain.port.out.IHealthUnitRepository;
import br.com.morbus.agendamento.domain.port.out.IProviderRepository;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultarGradeUseCaseTest {

    @Mock
    private IScheduleRepository scheduleRepository;

    @Mock
    private IHealthUnitRepository healthUnitRepository;

    @Mock
    private IProviderRepository providerRepository;

    private ConsultarGradeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarGradeUseCase(scheduleRepository, healthUnitRepository, providerRepository);
    }

    @Test
    void deveRetornarSchedulesAtivosParaODiaDaSemana() {
        UUID unitId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();

        HealthUnit unit = new HealthUnit(unitId, "UBS Central", "1234567", "Sao Paulo", "SP");
        Provider provider = new Provider(providerId, "Dr. Silva", "CRM-12345", "Clinica Geral");

        Schedule schedule = new Schedule(
                UUID.randomUUID(), unitId, providerId, UUID.randomUUID(),
                EDiaSemana.TERCA,
                LocalTime.of(8, 0), LocalTime.of(12, 0),
                30, 2, true
        );

        when(healthUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(scheduleRepository.findByUnitId(unitId)).thenReturn(List.of(schedule));
        when(providerRepository.findById(providerId)).thenReturn(Optional.of(provider));

        // 2026-07-07 é uma terça-feira
        List<GradeItem> resultado = useCase.execute(unitId, "2026-07-07");

        assertEquals(1, resultado.size());
        GradeItem item = resultado.get(0);
        assertEquals(schedule, item.schedule());
        assertEquals(unit, item.unit());
        assertEquals(provider, item.provider());

        verify(scheduleRepository).findByUnitId(unitId);
        verify(healthUnitRepository).findById(unitId);
        verify(providerRepository).findById(providerId);
    }

    @Test
    void deveRetornarProviderNuloQuandoScheduleNaoPossuiProvider() {
        UUID unitId = UUID.randomUUID();

        HealthUnit unit = new HealthUnit(unitId, "UBS Central", "1234567", "Sao Paulo", "SP");

        Schedule schedule = new Schedule(
                UUID.randomUUID(), unitId, null, UUID.randomUUID(),
                EDiaSemana.TERCA,
                LocalTime.of(8, 0), LocalTime.of(12, 0),
                30, 2, true
        );

        when(healthUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(scheduleRepository.findByUnitId(unitId)).thenReturn(List.of(schedule));

        List<GradeItem> resultado = useCase.execute(unitId, "2026-07-07");

        assertEquals(1, resultado.size());
        assertNull(resultado.get(0).provider());
        verifyNoInteractions(providerRepository);
    }

    @Test
    void deveIgnorarSchedulesInativos() {
        UUID unitId = UUID.randomUUID();

        HealthUnit unit = new HealthUnit(unitId, "UBS Central", "1234567", "Sao Paulo", "SP");

        Schedule scheduleInativo = new Schedule(
                UUID.randomUUID(), unitId, null, UUID.randomUUID(),
                EDiaSemana.TERCA,
                LocalTime.of(8, 0), LocalTime.of(12, 0),
                30, 2, false
        );

        when(healthUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(scheduleRepository.findByUnitId(unitId)).thenReturn(List.of(scheduleInativo));

        List<GradeItem> resultado = useCase.execute(unitId, "2026-07-07");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveIgnorarSchedulesDeOutrosDias() {
        UUID unitId = UUID.randomUUID();

        HealthUnit unit = new HealthUnit(unitId, "UBS Central", "1234567", "Sao Paulo", "SP");

        // Segunda-feira, mas week é terça (2026-07-07)
        Schedule scheduleTerca = new Schedule(
                UUID.randomUUID(), unitId, null, UUID.randomUUID(),
                EDiaSemana.SEGUNDA,
                LocalTime.of(8, 0), LocalTime.of(12, 0),
                30, 2, true
        );

        when(healthUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(scheduleRepository.findByUnitId(unitId)).thenReturn(List.of(scheduleTerca));

        List<GradeItem> resultado = useCase.execute(unitId, "2026-07-07");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaSchedulesParaODia() {
        UUID unitId = UUID.randomUUID();

        HealthUnit unit = new HealthUnit(unitId, "UBS Central", "1234567", "Sao Paulo", "SP");

        when(healthUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(scheduleRepository.findByUnitId(unitId)).thenReturn(List.of());

        List<GradeItem> resultado = useCase.execute(unitId, "2026-07-07");

        assertTrue(resultado.isEmpty());
        verifyNoInteractions(providerRepository);
    }

    @Test
    void deveRetornarOrdenadoPorHorarioInicio() {
        UUID unitId = UUID.randomUUID();

        HealthUnit unit = new HealthUnit(unitId, "UBS Central", "1234567", "Sao Paulo", "SP");

        Schedule tarde = new Schedule(
                UUID.randomUUID(), unitId, null, UUID.randomUUID(),
                EDiaSemana.TERCA,
                LocalTime.of(14, 0), LocalTime.of(18, 0),
                30, 2, true
        );
        Schedule manha = new Schedule(
                UUID.randomUUID(), unitId, null, UUID.randomUUID(),
                EDiaSemana.TERCA,
                LocalTime.of(8, 0), LocalTime.of(12, 0),
                30, 2, true
        );

        when(healthUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(scheduleRepository.findByUnitId(unitId)).thenReturn(List.of(tarde, manha));

        List<GradeItem> resultado = useCase.execute(unitId, "2026-07-07");

        assertEquals(2, resultado.size());
        assertEquals(LocalTime.of(8, 0), resultado.get(0).schedule().getHorarioInicio());
        assertEquals(LocalTime.of(14, 0), resultado.get(1).schedule().getHorarioInicio());
    }

    @Test
    void deveFalharQuandoUnidadeNaoEncontrada() {
        UUID unitId = UUID.randomUUID();

        when(healthUnitRepository.findById(unitId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(unitId, "2026-07-07")
        );

        assertEquals("Unidade nao encontrada: " + unitId, exception.getMessage());
        verifyNoInteractions(scheduleRepository);
        verifyNoInteractions(providerRepository);
    }

    @Test
    void deveFalharQuandoFormatoDaSemanaForInvalido() {
        UUID unitId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(unitId, "07/07/2026")
        );

        assertEquals("Formato de data invalido para 'week'. Use yyyy-MM-dd.", exception.getMessage());
        verifyNoInteractions(healthUnitRepository);
        verifyNoInteractions(scheduleRepository);
        verifyNoInteractions(providerRepository);
    }
}
