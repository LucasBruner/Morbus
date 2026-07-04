package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.application.command.CriarScheduleCommand;
import br.com.morbus.agendamento.application.command.CriarScheduleResult;
import br.com.morbus.agendamento.domain.exception.DuplicateScheduleException;
import br.com.morbus.agendamento.domain.exception.InvalidSchedulePeriodException;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.ICriarScheduleUseCase;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class CriarScheduleUseCase implements ICriarScheduleUseCase {

    private final IScheduleRepository scheduleRepository;
    private final ISlotRepository slotRepository;

    public CriarScheduleUseCase(IScheduleRepository scheduleRepository,
                                ISlotRepository slotRepository) {
        this.scheduleRepository = scheduleRepository;
        this.slotRepository = slotRepository;
    }

    @Override
    public CriarScheduleResult execute(CriarScheduleCommand command) {
        if (!command.horarioFim().isAfter(command.horarioInicio())) {
            throw new InvalidSchedulePeriodException("horarioFim deve ser maior que horarioInicio.");
        }

        if (scheduleRepository.existsByProviderIdAndUnitIdAndDiaDaSemana(
                command.providerId(), command.unitId(), command.diaDaSemana())) {
            throw new DuplicateScheduleException(
                    "Ja existe grade ativa para o providerId, unitId e diaDaSemana informados.");
        }

        Schedule schedule = scheduleRepository.save(new Schedule(
                command.unitId(),
                command.providerId(),
                command.procedureId(),
                command.diaDaSemana(),
                command.horarioInicio(),
                command.horarioFim(),
                command.slotDuracaoMinutos(),
                command.capacidade()
        ));

        List<Slot> slots = gerarSlots(schedule);
        slotRepository.saveAll(slots);

        return new CriarScheduleResult(schedule, slots.size());
    }

    private List<Slot> gerarSlots(Schedule schedule) {
        LocalDate proximaOcorrencia = LocalDate.now()
                .with(TemporalAdjusters.nextOrSame(schedule.getDiaDaSemana().toDayOfWeek()));

        List<Slot> slots = new ArrayList<>();
        LocalTime horario = schedule.getHorarioInicio();

        while (!horario.plusMinutes(schedule.getSlotDuracaoMinutos()).isAfter(schedule.getHorarioFim())) {
            slots.add(new Slot(
                    schedule.getId(),
                    LocalDateTime.of(proximaOcorrencia, horario),
                    schedule.getCapacidade()
            ));
            horario = horario.plusMinutes(schedule.getSlotDuracaoMinutos());
        }

        return slots;
    }
}
