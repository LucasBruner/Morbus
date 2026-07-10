package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.entity.QueueEntry;

public record QueueEntryRiskQueuePosition(QueueEntry queueEntry, int totalAhead) {
}
