package br.com.sus.notificationservice.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SolicitacaoNegadaEventDTO(UUID solicitacaoId,
                                        UUID patientId,
                                        UUID procedureId,
                                        String justificativa,
                                        LocalDateTime negadoEm) {
}
