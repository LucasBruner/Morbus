package br.com.morbus.agendamento.adapter.in.graphql.dto;

import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.IConsultarDisponibilidadeUseCase.SlotItem;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlotsAvailableResponseDTO(UUID id,
                                        LocalDateTime dataHora,
                                        Integer capacity,
                                        Integer booked,
                                        Integer remainingCapacity,
                                        EStatusSlots status,
                                        ScheduleResponseDTO schedule) {

    public static SlotsAvailableResponseDTO fromItem(SlotItem item) {
        Slot s = item.slot();
        return new SlotsAvailableResponseDTO(
                s.getId(),
                s.getDataHora(),
                s.getCapacidade(),
                s.getReservados(),
                s.getCapacidade() - s.getReservados(),
                s.getStatus(),
                ScheduleResponseDTO.from(item.schedule(), item.unit(), item.provider())
        );
    }
}
