package br.com.morbus.agendamento.adapter.in.graphql.dto;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.model.Agendamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentosPacienteResponseDTO(  UUID id,
                                                UUID slotId,
                                                EStatusAgendamento status,
                                                LocalDateTime expiresAt,
                                                LocalDateTime confirmedAt,
                                                LocalDateTime attendedAt,
                                                LocalDateTime noShowAt,
                                                String cancellationReason) {

    public static AgendamentosPacienteResponseDTO fromEntity(Agendamento a) {
        return new AgendamentosPacienteResponseDTO(
                a.getId(),
                a.getSlotId(),
                a.getStatus(),
                a.getExpiresAt(),
                a.getConfirmedAt(),
                a.getAttendedAt(),
                a.getNoShowAt(),
                a.getCancellationReason()
        );
    }
}
