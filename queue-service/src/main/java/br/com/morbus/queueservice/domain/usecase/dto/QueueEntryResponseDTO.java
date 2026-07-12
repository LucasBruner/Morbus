package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EDestino;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dados de uma entrada na fila")
public record QueueEntryResponseDTO(
        UUID id,
        PatientResponseDTO patient,
        ProcedureResponseDTO procedure,
        ERiskColor riskColor,
        String status,
        LocalDateTime registeredAt,
        EDestino tipoFila,
        EPriorityGroup priorityGroup,
        UUID solicitacaoId,
        UUID preferredUnitId,
        Integer position,
        LocalDateTime updatedAt,
        LocalDateTime calledAt,
        Integer newPosition
) {
    public static QueueEntryResponseDTO fromEntity(QueueEntry entry) {
        return fromEntity(entry, null);
    }

    public static QueueEntryResponseDTO fromEntity(QueueEntry entry, Integer position) {
        if (entry == null) return null;
        return new QueueEntryResponseDTO(
                entry.getId(),
                PatientResponseDTO.fromEntity(entry.getPatient()),
                ProcedureResponseDTO.fromEntity(entry.getProcedure()),
                entry.getRiskColor(),
                entry.getQueueStatus() != null ? entry.getQueueStatus().name() : null,
                entry.getRegisteredAt(),
                entry.getTipoFila(),
                entry.getPriorityGroup(),
                entry.getSolicitacaoId(),
                entry.getPreferredUnitId(),
                position,
                entry.getUpdatedAt(),
                null,
                null
        );
    }

    public static QueueEntryResponseDTO forCallNext(QueueEntry entry) {
        QueueEntryResponseDTO base = fromEntity(entry);
        return new QueueEntryResponseDTO(
                base.id(), base.patient(), base.procedure(), base.riskColor(), base.status(),
                base.registeredAt(), base.tipoFila(), base.priorityGroup(), base.solicitacaoId(),
                base.preferredUnitId(), base.position(), base.updatedAt(), entry.getUpdatedAt(), null
        );
    }

    public static QueueEntryResponseDTO forPriorityUpdate(QueueEntry entry, int newPosition) {
        QueueEntryResponseDTO base = fromEntity(entry);
        return new QueueEntryResponseDTO(
                base.id(), base.patient(), base.procedure(), base.riskColor(), base.status(),
                base.registeredAt(), base.tipoFila(), base.priorityGroup(), base.solicitacaoId(),
                base.preferredUnitId(), base.position(), base.updatedAt(), null, newPosition
        );
    }
}