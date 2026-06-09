package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.exception.QueueNotExistException;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.usecase.dto.QueueEntryRiskQueuePosition;

import java.util.UUID;

public class GetQueuePosition {
    private final IQueueEntryRepository queueEntryRepository;
    private GetQueuePosition(IQueueEntryRepository queueEntryRepository) {
        this.queueEntryRepository = queueEntryRepository;
    }

    public static GetQueuePosition create(IQueueEntryRepository queueEntryRepository) {
        return new GetQueuePosition(queueEntryRepository);
    }

    public QueueEntryRiskQueuePosition run(UUID id) {
        QueueEntry queueEntryById = queueEntryRepository
                .findById(id)
                .orElseThrow(() -> new QueueNotExistException("Não existe fila com esse ID"));
        int posicaoCalculada = queueEntryRepository.countEntriesWithHigherPriority(queueEntryById);
        return new QueueEntryRiskQueuePosition(queueEntryById, posicaoCalculada);
    }
}
