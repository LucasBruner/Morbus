package br.com.morbus.agendamento.domain.port.out;

import br.com.morbus.agendamento.domain.enums.EDiaSemana;
import br.com.morbus.agendamento.domain.model.Schedule;

import java.util.UUID;

public interface IScheduleRepository {

    Schedule save(Schedule schedule);

    boolean existsByProviderIdAndUnitIdAndDiaDaSemana(UUID providerId,
                                                      UUID unitId,
                                                      EDiaSemana diaDaSemana);
}
