package br.com.morbus.agendamento.domain.port.in;

import br.com.morbus.agendamento.domain.model.Schedule;

import java.time.LocalTime;
import java.util.UUID;

public interface IAtualizarScheduleUseCase {

    Schedule execute(UUID id,
                     UUID requesterUnitId,
                     UUID providerId,
                     LocalTime horarioInicio,
                     LocalTime horarioFim,
                     Integer capacidade);
}
