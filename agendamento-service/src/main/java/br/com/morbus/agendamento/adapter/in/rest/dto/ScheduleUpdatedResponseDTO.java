package br.com.morbus.agendamento.adapter.in.rest.dto;

import br.com.morbus.agendamento.domain.enums.EDiaSemana;
import br.com.morbus.agendamento.domain.model.Schedule;

import java.time.LocalTime;
import java.util.UUID;

public record ScheduleUpdatedResponseDTO(
        UUID id,
        UUID unitId,
        UUID providerId,
        UUID procedureId,
        EDiaSemana diaDaSemana,
        LocalTime horarioInicio,
        LocalTime horarioFim,
        int slotDuracaoMinutos,
        int capacidade,
        boolean ativo
) {

    public static ScheduleUpdatedResponseDTO fromDomain(Schedule schedule) {
        return new ScheduleUpdatedResponseDTO(
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
}
