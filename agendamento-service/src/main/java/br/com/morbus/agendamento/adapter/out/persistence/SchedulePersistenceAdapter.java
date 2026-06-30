package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.enums.ETurnos;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public class SchedulePersistenceAdapter implements IScheduleRepository {

    private final IScheduleJpaRepository scheduleJpaRepository;

    public SchedulePersistenceAdapter(IScheduleJpaRepository scheduleJpaRepository) {
        this.scheduleJpaRepository = scheduleJpaRepository;
    }

    @Override
    public Schedule save(Schedule schedule) {
        return toDomain(scheduleJpaRepository.save(toEntity(schedule)));
    }

    @Override
    public boolean existsByProviderIdAndDataInicioBetweenAndTurno(UUID providerId,
                                                                  LocalDateTime dataInicio,
                                                                  LocalDateTime dataFim,
                                                                  ETurnos turno) {
        return scheduleJpaRepository.existsByProviderIdAndDataInicioBetweenAndTurno(
                providerId,
                dataInicio,
                dataFim,
                turno
        );
    }

    private ScheduleEntity toEntity(Schedule schedule) {
        return new ScheduleEntity(
                schedule.getId(),
                schedule.getProviderId(),
                schedule.getUnitId(),
                schedule.getDataInicio(),
                schedule.getDataFim(),
                schedule.getTurno()
        );
    }

    private Schedule toDomain(ScheduleEntity entity) {
        return new Schedule(
                entity.getId(),
                entity.getProviderId(),
                entity.getUnitId(),
                entity.getDataInicio(),
                entity.getDataFim(),
                entity.getTurno()
        );
    }
}
