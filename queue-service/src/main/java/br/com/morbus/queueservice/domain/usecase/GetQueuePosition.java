package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.exceptions.QueueNotExistException;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;

import java.util.UUID;

public class GetQueuePosition {
    private final IQueueEntryRepository queueEntryRepository;
    private GetQueuePosition(IQueueEntryRepository queueEntryRepository) {
        this.queueEntryRepository = queueEntryRepository;
    }

    public static GetQueuePosition create(IQueueEntryRepository queueEntryRepository) {
        return new GetQueuePosition(queueEntryRepository);
    }

    public QueueEntry run(UUID id) {
        return queueEntryRepository
                .findById(id)
                .orElseThrow(() -> new QueueNotExistException("Não existe fila com esse ID"));
    }
}
