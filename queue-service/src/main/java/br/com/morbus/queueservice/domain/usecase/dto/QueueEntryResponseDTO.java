package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
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
        LocalDateTime registeredAt
) {
    public static QueueEntryResponseDTO fromEntity(QueueEntry entry) {
        if (entry == null) return null;
        return new QueueEntryResponseDTO(
                entry.getId(),
                PatientResponseDTO.fromEntity(entry.getPatient()),
                ProcedureResponseDTO.fromEntity(entry.getProcedure()),
                entry.getRiskColor(),
                entry.getStatus() != null ? entry.getStatus().name() : null,
                entry.getRegisteredAt()
        );
    }
}