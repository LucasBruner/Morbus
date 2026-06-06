package br.com.morbus.queueservice.domain.usecase.DTO;

import br.com.morbus.queueservice.domain.enums.ERiskColor;

import java.util.UUID;

public record QueueUpdateRiskColorDTO(UUID queueId, ERiskColor riskColor) {
}
