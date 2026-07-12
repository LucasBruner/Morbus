package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Posição atual de uma entrada na fila")
public record QueuePositionResponseDTO(
        UUID id,
        int position,
        int totalAhead,
        QueuePositionPatientDTO patient,
        QueuePositionProcedureDTO procedure,
        ERiskColor riskColor,
        EPriorityGroup priorityGroup,
        String status,
        Integer estimatedWaitMonths,
        LocalDateTime registeredAt
) {
    public static QueuePositionResponseDTO from(QueueEntry entry, int totalAhead) {
        return new QueuePositionResponseDTO(
                entry.getId(),
                totalAhead + 1,
                totalAhead,
                QueuePositionPatientDTO.fromEntity(entry.getPatient()),
                QueuePositionProcedureDTO.fromEntity(entry.getProcedure()),
                entry.getRiskColor(),
                entry.getPriorityGroup(),
                entry.getQueueStatus() != null ? entry.getQueueStatus().name() : null,
                estimatedWaitMonths(entry.getRiskColor()),
                entry.getRegisteredAt()
        );
    }

    private static Integer estimatedWaitMonths(ERiskColor riskColor) {
        if (riskColor == null) return null;
        return switch (riskColor) {
            case VERMELHO -> 1;
            case AMARELO -> 3;
            case VERDE -> 6;
            case AZUL -> 12;
        };
    }
}
