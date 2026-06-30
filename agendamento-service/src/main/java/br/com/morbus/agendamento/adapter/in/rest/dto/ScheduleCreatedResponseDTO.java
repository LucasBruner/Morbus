package br.com.morbus.agendamento.adapter.in.rest.dto;

import br.com.morbus.agendamento.application.command.CriarScheduleResult;
import br.com.morbus.agendamento.domain.enums.ETurnos;
import br.com.morbus.agendamento.domain.model.Schedule;

import java.time.LocalDateTime;
import java.util.UUID;

public record ScheduleCreatedResponseDTO(
        UUID id,
        UUID providerId,
        UUID unitId,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        ETurnos turno,
        int slotsGerados
) {

    public static ScheduleCreatedResponseDTO fromResult(CriarScheduleResult result) {
        Schedule schedule = result.schedule();
        return new ScheduleCreatedResponseDTO(
                schedule.getId(),
                schedule.getProviderId(),
                schedule.getUnitId(),
                schedule.getDataInicio(),
                schedule.getDataFim(),
                schedule.getTurno(),
                result.slotsGerados()
        );
    }
}
