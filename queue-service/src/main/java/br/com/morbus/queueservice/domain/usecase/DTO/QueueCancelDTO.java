package br.com.morbus.queueservice.domain.usecase.DTO;

import java.util.UUID;

public record QueueCancelDTO(UUID queueId, String reason) {
}
