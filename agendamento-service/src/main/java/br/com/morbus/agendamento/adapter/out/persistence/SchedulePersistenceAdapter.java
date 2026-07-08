package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.enums.EDiaSemana;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
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
    public Optional<Schedule> findById(UUID id) {
        return scheduleJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByProviderIdAndUnitIdAndDiaDaSemana(UUID providerId,
                                                             UUID unitId,
                                                             EDiaSemana diaDaSemana) {
        return scheduleJpaRepository.existsByProviderIdAndUnitIdAndDiaDaSemana(
                providerId, unitId, diaDaSemana);
    }

    @Override
    public List<Schedule> findByUnitId(UUID unitId) {
        return scheduleJpaRepository.findByUnitId(unitId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private ScheduleEntity toEntity(Schedule schedule) {
        return new ScheduleEntity(
                schedule.getId(),
                schedule.getUnitId(),
                schedule.getProviderId(),
                schedule.getProcedureId(),
                schedule.getDiaDaSemana(),
                schedule.getHorarioInicio(),
                schedule.getHorarioFim(),
                schedule.getSlotDuracaoMinutos(),
                schedule.getCapacidade(),
                schedule.isAtivo()
        );
    }

    private Schedule toDomain(ScheduleEntity entity) {
        return new Schedule(
                entity.getId(),
                entity.getUnitId(),
                entity.getProviderId(),
                entity.getProcedureId(),
                entity.getDiaDaSemana(),
                entity.getHorarioInicio(),
                entity.getHorarioFim(),
                entity.getSlotDuracaoMinutos(),
                entity.getCapacidade(),
                entity.isAtivo()
        );
    }
}
