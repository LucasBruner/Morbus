package br.com.sus.notificationservice.model.dto;

import java.util.UUID;

public record SolicitacaoDevolvidaEventDTO(UUID solicitacaoId,
                                           UUID patientId,
                                           UUID procedureId,
                                           String justificativa) {
}
