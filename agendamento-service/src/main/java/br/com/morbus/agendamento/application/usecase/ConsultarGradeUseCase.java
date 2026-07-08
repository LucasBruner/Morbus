package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.model.HealthUnit;
import br.com.morbus.agendamento.domain.model.Provider;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.port.in.IConsultarGradeUseCase;
import br.com.morbus.agendamento.domain.port.out.IHealthUnitRepository;
import br.com.morbus.agendamento.domain.port.out.IProviderRepository;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ConsultarGradeUseCase implements IConsultarGradeUseCase {

    private final IScheduleRepository scheduleRepository;
    private final IHealthUnitRepository healthUnitRepository;
    private final IProviderRepository providerRepository;

    public ConsultarGradeUseCase(IScheduleRepository scheduleRepository,
                                 IHealthUnitRepository healthUnitRepository,
                                 IProviderRepository providerRepository) {
        this.scheduleRepository = scheduleRepository;
        this.healthUnitRepository = healthUnitRepository;
        this.providerRepository = providerRepository;
    }

    @Override
    public List<GradeItem> execute(UUID unitId, String week) {
        parseWeek(week);

        HealthUnit unit = healthUnitRepository.findById(unitId).orElseThrow(
                () -> new IllegalArgumentException("Unidade nao encontrada: " + unitId));

        List<Schedule> schedule = scheduleRepository.findByUnitId(unitId)
                .stream()
                .filter(s ->
                        s.isAtivo() &&
                        s.getDiaDaSemana().toDayOfWeek().equals(LocalDate.parse(week).getDayOfWeek()))
                .sorted(Comparator
                        .comparing(Schedule::getDiaDaSemana)
                        .thenComparing(Schedule::getHorarioInicio))
                .toList();

        if (schedule.isEmpty()) return List.of();

        return schedule
                .stream()
                .map(s -> {
                    Provider provider = s.getProviderId() != null
                            ? providerRepository.findById(s.getProviderId()).orElse(null)
                            : null;
                    return new GradeItem(s, unit, provider);
                })
                .toList();
    }

    private void parseWeek(String week) {
        try {
            LocalDate.parse(week);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Formato de data invalido para 'week'. Use yyyy-MM-dd.", ex);
        }
    }
}
