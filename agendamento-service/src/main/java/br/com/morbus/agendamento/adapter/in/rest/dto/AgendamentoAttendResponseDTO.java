package br.com.morbus.agendamento.adapter.in.rest.dto;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.model.Agendamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentoAttendResponseDTO(
        UUID id,
        EStatusAgendamento status,
        LocalDateTime attendedAt
) {

    public static AgendamentoAttendResponseDTO fromEntity(Agendamento a) {
        return new AgendamentoAttendResponseDTO(
                a.getId(),
                a.getStatus(),
                a.getAttendedAt()
        );
    }
}
