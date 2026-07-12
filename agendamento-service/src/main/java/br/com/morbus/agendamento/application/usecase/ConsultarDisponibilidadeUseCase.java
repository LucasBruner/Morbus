package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.model.HealthUnit;
import br.com.morbus.agendamento.domain.model.Provider;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.IConsultarDisponibilidadeUseCase;
import br.com.morbus.agendamento.domain.port.out.IHealthUnitRepository;
import br.com.morbus.agendamento.domain.port.out.IProviderRepository;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConsultarDisponibilidadeUseCase implements IConsultarDisponibilidadeUseCase {

    private final ISlotRepository slotRepository;
    private final IScheduleRepository scheduleRepository;
    private final IHealthUnitRepository healthUnitRepository;
    private final IProviderRepository providerRepository;

    public ConsultarDisponibilidadeUseCase(ISlotRepository slotRepository,
                                           IScheduleRepository scheduleRepository,
                                           IHealthUnitRepository healthUnitRepository,
                                           IProviderRepository providerRepository) {
        this.slotRepository = slotRepository;
        this.scheduleRepository = scheduleRepository;
        this.healthUnitRepository = healthUnitRepository;
        this.providerRepository = providerRepository;
    }

    @Override
    public List<SlotItem> execute(UUID procedureId,
                                  UUID unitId,
                                  String dateFrom,
                                  String dateTo) {
        LocalDateTime parsedDateFrom = parseDate(dateFrom, false);
        LocalDateTime parsedDateTo = parseDate(dateTo, true);

        if (parsedDateFrom.isAfter(parsedDateTo)) {
            throw new IllegalArgumentException("dateFrom deve ser anterior ou igual a dateTo");
        }

        List<Slot> slots = slotRepository.findByProcedureAndUnitAndDate(procedureId, unitId, parsedDateFrom, parsedDateTo);

        Map<UUID, Schedule> scheduleCache = new HashMap<>();
        Map<UUID, HealthUnit> unitCache = new HashMap<>();
        Map<UUID, Provider> providerCache = new HashMap<>();

        return slots.stream()
                .map(slot -> {
                    Schedule schedule = scheduleCache.computeIfAbsent(slot.getScheduleId(),
                            id -> scheduleRepository.findById(id).orElseThrow(
                                    () -> new IllegalStateException("Grade nao encontrada para o slot: " + slot.getId())));
                    HealthUnit unit = unitCache.computeIfAbsent(schedule.getUnitId(),
                            id -> healthUnitRepository.findById(id).orElseThrow(
                                    () -> new IllegalStateException("Unidade nao encontrada: " + id)));
                    Provider provider = schedule.getProviderId() != null
                            ? providerCache.computeIfAbsent(schedule.getProviderId(),
                                    id -> providerRepository.findById(id).orElse(null))
                            : null;
                    return new SlotItem(slot, schedule, unit, provider);
                })
                .toList();
    }

    private LocalDateTime parseDate(String value, boolean endOfDay) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                LocalDate date = LocalDate.parse(value);
                return endOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Formato de data invalido. Use ISO-8601 em dateFrom/dateTo.", ex);
            }
        }
    }
}