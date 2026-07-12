package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.exception.ScheduleNotFoundException;
import br.com.morbus.agendamento.domain.exception.SlotNotFoundException;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnblockSlotUseCaseTest {

    @Mock
    private ISlotRepository slotRepository;

    @Mock
    private IScheduleRepository scheduleRepository;

    private UnblockSlotUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UnblockSlotUseCase(slotRepository, scheduleRepository);
    }

    @Test
    void deveDesbloquearSlotsIndisponiveis() {
        UUID scheduleId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        Schedule schedule = new Schedule(unitId, UUID.randomUUID(), UUID.randomUUID(), null, LocalTime.NOON, LocalTime.MIDNIGHT, 30, 1);
        Slot slotBloqueado = new Slot(UUID.randomUUID(), scheduleId, LocalDateTime.now(), 1, 0, EStatusSlots.INDISPONIVEL);
        Slot slotDisponivel = new Slot(UUID.randomUUID(), scheduleId, LocalDateTime.now(), 1, 0, EStatusSlots.DISPONIVEL);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(slotRepository.findByScheduleId(scheduleId)).thenReturn(List.of(slotBloqueado, slotDisponivel));

        useCase.execute(scheduleId, unitId);

        assertEquals(EStatusSlots.DISPONIVEL, slotBloqueado.getStatus());
        verify(slotRepository).saveAll(List.of(slotBloqueado));
    }

    @Test
    void deveLancarExcecaoQuandoScheduleNaoEncontrada() {
        UUID scheduleId = UUID.randomUUID();
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        assertThrows(ScheduleNotFoundException.class, () -> useCase.execute(scheduleId, UUID.randomUUID()));

        verify(slotRepository, never()).saveAll(anyList());
    }

    @Test
    void deveLancarAccessDeniedQuandoUnitIdNaoPertenceAGrade() {
        UUID scheduleId = UUID.randomUUID();
        Schedule schedule = new Schedule(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, LocalTime.NOON, LocalTime.MIDNIGHT, 30, 1);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));

        assertThrows(AccessDeniedException.class, () -> useCase.execute(scheduleId, UUID.randomUUID()));

        verify(slotRepository, never()).saveAll(anyList());
    }

    @Test
    void deveLancarExcecaoQuandoNaoHaSlotsIndisponiveis() {
        UUID scheduleId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        Schedule schedule = new Schedule(unitId, UUID.randomUUID(), UUID.randomUUID(), null, LocalTime.NOON, LocalTime.MIDNIGHT, 30, 1);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(slotRepository.findByScheduleId(scheduleId)).thenReturn(List.of());

        assertThrows(SlotNotFoundException.class, () -> useCase.execute(scheduleId, unitId));
    }
}
