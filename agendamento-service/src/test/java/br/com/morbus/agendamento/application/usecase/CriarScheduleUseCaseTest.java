package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.application.command.CriarScheduleCommand;
import br.com.morbus.agendamento.application.command.CriarScheduleResult;
import br.com.morbus.agendamento.domain.enums.EDiaSemana;
import br.com.morbus.agendamento.domain.exception.DuplicateScheduleException;
import br.com.morbus.agendamento.domain.exception.InvalidSchedulePeriodException;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriarScheduleUseCaseTest {

    @Mock
    private IScheduleRepository scheduleRepository;

    @Mock
    private ISlotRepository slotRepository;

    private CriarScheduleUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CriarScheduleUseCase(scheduleRepository, slotRepository);
    }

    @Test
    void deveCriarScheduleEGerarSlots() {
        UUID providerId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID procedureId = UUID.randomUUID();
        CriarScheduleCommand command = new CriarScheduleCommand(
                providerId, unitId, procedureId, EDiaSemana.SEGUNDA,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 30, 2);

        when(scheduleRepository.existsByProviderIdAndUnitIdAndDiaDaSemana(providerId, unitId, EDiaSemana.SEGUNDA))
                .thenReturn(false);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(slotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        CriarScheduleResult result = useCase.execute(command);

        assertEquals(2, result.slotsGerados());
        assertEquals(unitId, result.schedule().getUnitId());

        ArgumentCaptor<List<Slot>> captor = ArgumentCaptor.forClass(List.class);
        verify(slotRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    void deveLancarExcecaoQuandoHorarioFimNaoEMaiorQueHorarioInicio() {
        CriarScheduleCommand command = new CriarScheduleCommand(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), EDiaSemana.SEGUNDA,
                LocalTime.of(9, 0), LocalTime.of(8, 0), 30, 1);

        assertThrows(InvalidSchedulePeriodException.class, () -> useCase.execute(command));

        verifyNoInteractions(scheduleRepository, slotRepository);
    }

    @Test
    void deveLancarExcecaoQuandoJaExisteGradeAtivaParaProviderUnitEDia() {
        UUID providerId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        CriarScheduleCommand command = new CriarScheduleCommand(
                providerId, unitId, UUID.randomUUID(), EDiaSemana.SEGUNDA,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 30, 1);

        when(scheduleRepository.existsByProviderIdAndUnitIdAndDiaDaSemana(providerId, unitId, EDiaSemana.SEGUNDA))
                .thenReturn(true);

        assertThrows(DuplicateScheduleException.class, () -> useCase.execute(command));

        verify(scheduleRepository, never()).save(any());
        verifyNoInteractions(slotRepository);
    }
}
