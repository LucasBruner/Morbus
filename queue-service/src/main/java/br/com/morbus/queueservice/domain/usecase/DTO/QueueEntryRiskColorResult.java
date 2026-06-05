package br.com.morbus.queueservice.domain.usecase.DTO;

import br.com.morbus.queueservice.domain.entity.QueueEntry;

public record QueueEntryRiskColorResult(QueueEntry queueEntry, int posicaoCalculada) {
}
