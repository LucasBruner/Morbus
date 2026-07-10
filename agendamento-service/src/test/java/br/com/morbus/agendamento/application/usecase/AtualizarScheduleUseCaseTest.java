package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.exception.InvalidSchedulePeriodException;
import br.com.morbus.agendamento.domain.exception.ScheduleNotFoundException;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtualizarScheduleUseCaseTest {

    @Mock
    private IScheduleRepository scheduleRepository;

    private AtualizarScheduleUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AtualizarScheduleUseCase(scheduleRepository);
    }

    @Test
    void deveAtualizarHorarioCapacidadeEProfissional() {
        UUID scheduleId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID novoProviderId = UUID.randomUUID();
        Schedule existente = new Schedule(scheduleId, unitId, UUID.randomUUID(), UUID.randomUUID(),
                br.com.morbus.agendamento.domain.enums.EDiaSemana.SEGUNDA,
                LocalTime.of(8, 0), LocalTime.of(12, 0), 30, 2, true);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(existente));
        when(scheduleRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        Schedule resultado = useCase.execute(scheduleId, unitId, novoProviderId,
                LocalTime.of(9, 0), LocalTime.of(13, 0), 3);

        assertEquals(novoProviderId, resultado.getProviderId());
        assertEquals(LocalTime.of(9, 0), resultado.getHorarioInicio());
        assertEquals(LocalTime.of(13, 0), resultado.getHorarioFim());
        assertEquals(3, resultado.getCapacidade());
        assertEquals(scheduleId, resultado.getId());

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(captor.capture());
        assertEquals(unitId, captor.getValue().getUnitId());
    }

    @Test
    void deveLancarExcecaoQuandoGradeNaoExiste() {
        UUID scheduleId = UUID.randomUUID();
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        assertThrows(ScheduleNotFoundException.class,
                () -> useCase.execute(scheduleId, UUID.randomUUID(), UUID.randomUUID(),
                        LocalTime.of(8, 0), LocalTime.of(12, 0), 2));
    }

    @Test
    void deveLancarExcecaoQuandoUnidadeNaoEDona() {
        UUID scheduleId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        Schedule existente = new Schedule(unitId, UUID.randomUUID(), UUID.randomUUID(),
                br.com.morbus.agendamento.domain.enums.EDiaSemana.SEGUNDA,
                LocalTime.of(8, 0), LocalTime.of(12, 0), 30, 2);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(existente));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(scheduleId, UUID.randomUUID(), UUID.randomUUID(),
                        LocalTime.of(9, 0), LocalTime.of(13, 0), 3));
    }

    @Test
    void deveLancarExcecaoQuandoHorarioFimNaoEDepoisDoInicio() {
        UUID scheduleId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        Schedule existente = new Schedule(scheduleId, unitId, UUID.randomUUID(), UUID.randomUUID(),
                br.com.morbus.agendamento.domain.enums.EDiaSemana.SEGUNDA,
                LocalTime.of(8, 0), LocalTime.of(12, 0), 30, 2, true);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(existente));

        assertThrows(InvalidSchedulePeriodException.class,
                () -> useCase.execute(scheduleId, unitId, UUID.randomUUID(),
                        LocalTime.of(13, 0), LocalTime.of(9, 0), 3));
    }
}
