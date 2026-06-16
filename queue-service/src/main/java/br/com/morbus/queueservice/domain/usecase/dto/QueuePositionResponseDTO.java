package br.com.morbus.queueservice.domain.usecase.dto;

public record QueuePositionResponseDTO(int position, QueueEntryResponseDTO queueEntry
) {}
