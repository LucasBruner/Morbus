package br.com.morbus.agendamento.adapter.in.rest.dto;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.model.Agendamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentoCreatedResponseDTO(
        UUID id,
        UUID queueEntryId,
        UUID slotId,
        UUID pacienteId,
        EStatusAgendamento status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt) {

    public static AgendamentoCreatedResponseDTO fromDomain(Agendamento a) {
        return new AgendamentoCreatedResponseDTO(
                a.getId(),
                a.getQueueEntryId(),
                a.getSlotId(),
                a.getPacienteId(),
                a.getStatus(),
                a.getExpiresAt(),
                a.getCreatedAt()
        );
    }
}
