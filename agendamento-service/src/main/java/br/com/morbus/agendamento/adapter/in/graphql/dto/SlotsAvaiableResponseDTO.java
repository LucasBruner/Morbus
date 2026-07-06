package br.com.morbus.agendamento.adapter.in.graphql.dto;

import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.model.Slot;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlotsAvaiableResponseDTO( UUID id,
                                        UUID scheduleId,
                                        LocalDateTime dataHora,
                                        Integer capacidade,
                                        Integer reservados,
                                        EStatusSlots status) {

    public static SlotsAvaiableResponseDTO fromEntity(Slot s) {
        return new SlotsAvaiableResponseDTO(
                s.getId(),
                s.getScheduleId(),
                s.getDataHora(),
                s.getCapacidade(),
                s.getReservados(),
                s.getStatus()
        );
    }
}
