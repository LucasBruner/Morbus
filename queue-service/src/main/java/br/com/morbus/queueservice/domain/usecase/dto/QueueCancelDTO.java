package br.com.morbus.queueservice.domain.usecase.dto;

import java.util.UUID;

public record QueueCancelDTO(UUID queueId, String reason) {
}
