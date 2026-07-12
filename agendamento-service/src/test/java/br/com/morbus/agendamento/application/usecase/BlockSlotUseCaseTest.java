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

import java.time.LocalDate;
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
class BlockSlotUseCaseTest {

    @Mock
    private ISlotRepository slotRepository;

    @Mock
    private IScheduleRepository scheduleRepository;

    private BlockSlotUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BlockSlotUseCase(slotRepository, scheduleRepository);
    }

    @Test
    void deveBloquearSlotsDisponiveisNaDataInformada() {
        UUID scheduleId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        LocalDate date = LocalDate.now();
        Schedule schedule = new Schedule(unitId, UUID.randomUUID(), UUID.randomUUID(), null, LocalTime.NOON, LocalTime.MIDNIGHT, 30, 1);
        Slot slotNaData = new Slot(UUID.randomUUID(), scheduleId, LocalDateTime.of(date, LocalTime.of(8, 0)), 1, 0, EStatusSlots.DISPONIVEL);
        Slot slotOutraData = new Slot(UUID.randomUUID(), scheduleId, LocalDateTime.of(date.plusDays(1), LocalTime.of(8, 0)), 1, 0, EStatusSlots.DISPONIVEL);
        Slot slotIndisponivel = new Slot(UUID.randomUUID(), scheduleId, LocalDateTime.of(date, LocalTime.of(9, 0)), 1, 0, EStatusSlots.INDISPONIVEL);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(slotRepository.findByScheduleId(scheduleId)).thenReturn(List.of(slotNaData, slotOutraData, slotIndisponivel));

        useCase.execute(scheduleId, unitId, date, "Manutencao");

        assertEquals(EStatusSlots.INDISPONIVEL, slotNaData.getStatus());
        assertEquals(EStatusSlots.DISPONIVEL, slotOutraData.getStatus());
        verify(slotRepository).saveAll(List.of(slotNaData));
    }

    @Test
    void deveLancarExcecaoQuandoScheduleNaoEncontrada() {
        UUID scheduleId = UUID.randomUUID();
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        assertThrows(ScheduleNotFoundException.class,
                () -> useCase.execute(scheduleId, UUID.randomUUID(), LocalDate.now(), "motivo"));

        verify(slotRepository, never()).saveAll(anyList());
    }

    @Test
    void deveLancarAccessDeniedQuandoUnitIdNaoPertenceAGrade() {
        UUID scheduleId = UUID.randomUUID();
        Schedule schedule = new Schedule(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, LocalTime.NOON, LocalTime.MIDNIGHT, 30, 1);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(scheduleId, UUID.randomUUID(), LocalDate.now(), "motivo"));

        verify(slotRepository, never()).saveAll(anyList());
    }

    @Test
    void deveLancarExcecaoQuandoNaoHaSlotsDisponiveisNaData() {
        UUID scheduleId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        Schedule schedule = new Schedule(unitId, UUID.randomUUID(), UUID.randomUUID(), null, LocalTime.NOON, LocalTime.MIDNIGHT, 30, 1);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(slotRepository.findByScheduleId(scheduleId)).thenReturn(List.of());

        assertThrows(SlotNotFoundException.class,
                () -> useCase.execute(scheduleId, unitId, LocalDate.now(), "motivo"));
    }
}
