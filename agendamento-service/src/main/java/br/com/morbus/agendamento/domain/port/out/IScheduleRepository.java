package br.com.morbus.agendamento.domain.port.out;

import br.com.morbus.agendamento.domain.enums.EDiaSemana;
import br.com.morbus.agendamento.domain.model.Schedule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IScheduleRepository {

    Schedule save(Schedule schedule);

    Optional<Schedule> findById(UUID id);

    boolean existsByProviderIdAndUnitIdAndDiaDaSemana(UUID providerId,
                                                      UUID unitId,
                                                      EDiaSemana diaDaSemana);

    List<Schedule> findByUnitId(UUID unitId);
}
