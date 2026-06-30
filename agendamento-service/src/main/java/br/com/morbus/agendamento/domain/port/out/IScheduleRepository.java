package br.com.morbus.agendamento.domain.port.out;

import br.com.morbus.agendamento.domain.enums.ETurnos;
import br.com.morbus.agendamento.domain.model.Schedule;

import java.time.LocalDateTime;
import java.util.UUID;

public interface IScheduleRepository {

    Schedule save(Schedule schedule);

    boolean existsByProviderIdAndDataInicioBetweenAndTurno(UUID providerId,
                                                           LocalDateTime dataInicio,
                                                           LocalDateTime dataFim,
                                                           ETurnos turno);
}
