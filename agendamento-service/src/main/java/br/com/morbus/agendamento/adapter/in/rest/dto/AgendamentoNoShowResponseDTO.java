package br.com.morbus.agendamento.adapter.in.rest.dto;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.model.Agendamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentoNoShowResponseDTO(
        UUID id,
        EStatusAgendamento status,
        LocalDateTime noShowAtt
) {

    public static AgendamentoNoShowResponseDTO fromEntity(Agendamento a) {
        return new AgendamentoNoShowResponseDTO(
                a.getId(),
                a.getStatus(),
                a.getNoShowAt()
        );
    }
}
