package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.application.command.CriarScheduleCommand;
import br.com.morbus.agendamento.application.command.CriarScheduleResult;
import br.com.morbus.agendamento.domain.enums.EDiaSemana;
import br.com.morbus.agendamento.domain.exception.DuplicateScheduleException;
import br.com.morbus.agendamento.domain.exception.InvalidSchedulePeriodException;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.ICriarScheduleUseCase;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
        validate(command);

        LocalDateTime dataInicio = command.dataInicio().atTime(command.horarioInicio());
        LocalDateTime dataFim = command.dataFim().atTime(command.horarioFim());

        if (scheduleRepository.existsByProviderIdAndDataInicioBetweenAndTurno(
                command.providerId(),
                command.dataInicio().atStartOfDay(),
                command.dataInicio().plusDays(1).atStartOfDay().minusNanos(1),
                command.turno())) {
            throw new DuplicateScheduleException("Ja existe grade para o providerId, dataInicio e turno informados.");
        }

        Schedule schedule = scheduleRepository.save(new Schedule(
                command.providerId(),
                command.unitId(),
                dataInicio,
                dataFim,
                command.turno()
        ));

        List<Slot> slots = gerarSlots(schedule.getId(), command);
        slotRepository.saveAll(slots);

        return new CriarScheduleResult(schedule, slots.size());
    }

    private void validate(CriarScheduleCommand command) {
        if (command.dataFim().isBefore(command.dataInicio())
                || command.dataFim().isEqual(command.dataInicio())) {
            throw new InvalidSchedulePeriodException("dataFim deve ser maior ou igual a dataInicio.");
        }

        if (command.slotDuracaoMinutos() == null || command.slotDuracaoMinutos() <= 0) {
            throw new InvalidSchedulePeriodException("slotDuracaoMinutos deve ser maior que zero.");
        }

        if (command.diasDaSemana() == null || command.diasDaSemana().isEmpty()) {
            throw new InvalidSchedulePeriodException("diasDaSemana deve possuir ao menos um dia.");
        }
    }

    private List<Slot> gerarSlots(UUID scheduleId, CriarScheduleCommand command) {
        Set<DayOfWeek> diasPermitidos = new HashSet<>(
                command.diasDaSemana().stream()
                        .map(EDiaSemana::toDayOfWeek)
                        .toList()
        );

        List<Slot> slots = new ArrayList<>();
        LocalDate data = command.dataInicio();

        while (!data.isAfter(command.dataFim())) {
            if (diasPermitidos.contains(data.getDayOfWeek())) {
                adicionarSlotsDoDia(slots, scheduleId, data, command);
            }
            data = data.plusDays(1);
        }

        return slots;
    }

    private void adicionarSlotsDoDia(List<Slot> slots,
                                     UUID scheduleId,
                                     LocalDate data,
                                     CriarScheduleCommand command) {
        LocalTime horario = command.horarioInicio();

        while (!horario.plusMinutes(command.slotDuracaoMinutos()).isAfter(command.horarioFim())) {
            slots.add(new Slot(
                    scheduleId,
                    LocalDateTime.of(data, horario),
                    command.slotDuracaoMinutos()
            ));
            horario = horario.plusMinutes(command.slotDuracaoMinutos());
        }
    }
}
