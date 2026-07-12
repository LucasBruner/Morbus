package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.enums.EDiaSemana;
import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.model.HealthUnit;
import br.com.morbus.agendamento.domain.model.Provider;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.IConsultarDisponibilidadeUseCase.SlotItem;
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
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarDisponibilidadeUseCaseTest {

    @Mock
    private ISlotRepository slotRepository;

    @Mock
    private IScheduleRepository scheduleRepository;

    @Mock
    private IHealthUnitRepository healthUnitRepository;

    @Mock
    private IProviderRepository providerRepository;

    private ConsultarDisponibilidadeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarDisponibilidadeUseCase(slotRepository, scheduleRepository, healthUnitRepository, providerRepository);
    }

    @Test
    void deveConsultarDisponibilidadeComIntervaloExpandidoParaDatas() {
        UUID procedureId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        List<Slot> esperado = List.of();

        when(slotRepository.findByProcedureAndUnitAndDate(
                procedureId,
                unitId,
            LocalDateTime.of(2026, Month.JULY, 5, 0, 0),
            LocalDateTime.of(2026, Month.JULY, 6, 23, 59, 59, 999999999)
        )).thenReturn(esperado);

        List<SlotItem> resultado = useCase.execute(procedureId, unitId, "2026-07-05", "2026-07-06");

        assertEquals(0, resultado.size());
        verify(slotRepository).findByProcedureAndUnitAndDate(
                procedureId,
                unitId,
            LocalDateTime.of(2026, Month.JULY, 5, 0, 0),
            LocalDateTime.of(2026, Month.JULY, 6, 23, 59, 59, 999999999)
        );
    }

    @Test
    void deveJuntarScheduleUnitEProviderParaCadaSlot() {
        UUID procedureId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();

        Slot slot = new Slot(UUID.randomUUID(), scheduleId,
                LocalDateTime.of(2026, Month.JULY, 5, 8, 30), 2, 0, EStatusSlots.DISPONIVEL);
        Schedule schedule = new Schedule(scheduleId, unitId, providerId, procedureId,
                EDiaSemana.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(12, 0), 30, 2, true);
        HealthUnit unit = new HealthUnit(unitId, "UPA Norte", "2077485", "São Paulo", "SP");
        Provider provider = new Provider(providerId, "Dr. Carlos Melo", "CRM/SP 98765", "Cardiologia");

        when(slotRepository.findByProcedureAndUnitAndDate(
                procedureId, unitId,
                LocalDateTime.of(2026, Month.JULY, 5, 0, 0),
                LocalDateTime.of(2026, Month.JULY, 5, 23, 59, 59, 999999999)
        )).thenReturn(List.of(slot));
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(healthUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(providerRepository.findById(providerId)).thenReturn(Optional.of(provider));

        List<SlotItem> resultado = useCase.execute(procedureId, unitId, "2026-07-05", "2026-07-05");

        assertEquals(1, resultado.size());
        SlotItem item = resultado.get(0);
        assertEquals(slot, item.slot());
        assertEquals(schedule, item.schedule());
        assertEquals(unit, item.unit());
        assertEquals(provider, item.provider());
    }

    @Test
    void deveRetornarProviderNuloQuandoScheduleNaoTemProfissional() {
        UUID procedureId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        Slot slot = new Slot(UUID.randomUUID(), scheduleId,
                LocalDateTime.of(2026, Month.JULY, 5, 8, 30), 2, 0, EStatusSlots.DISPONIVEL);
        Schedule schedule = new Schedule(scheduleId, unitId, null, procedureId,
                EDiaSemana.SEGUNDA, LocalTime.of(8, 0), LocalTime.of(12, 0), 30, 2, true);
        HealthUnit unit = new HealthUnit(unitId, "UPA Norte", "2077485", "São Paulo", "SP");

        when(slotRepository.findByProcedureAndUnitAndDate(
                procedureId, unitId,
                LocalDateTime.of(2026, Month.JULY, 5, 0, 0),
                LocalDateTime.of(2026, Month.JULY, 5, 23, 59, 59, 999999999)
        )).thenReturn(List.of(slot));
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(healthUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));

        List<SlotItem> resultado = useCase.execute(procedureId, unitId, "2026-07-05", "2026-07-05");

        assertEquals(1, resultado.size());
        assertEquals(null, resultado.get(0).provider());
    }

    @Test
    void deveFalharQuandoIntervaloForInvalido() {
        UUID procedureId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
            () -> useCase.execute(procedureId, null, "2026-07-07", "2026-07-06")
        );

        assertEquals("dateFrom deve ser anterior ou igual a dateTo", exception.getMessage());
    }

    @Test
    void deveFalharQuandoFormatoDeDataForInvalido() {
        UUID procedureId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
            () -> useCase.execute(procedureId, null, "07/05/2026", "2026-07-06")
        );

        assertEquals("Formato de data invalido. Use ISO-8601 em dateFrom/dateTo.", exception.getMessage());
    }
}
