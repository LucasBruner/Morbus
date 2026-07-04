package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.domain.exception.QueueNotAllowedException;
import br.com.morbus.queueservice.domain.exception.QueueNotExistException;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;

import java.util.UUID;

public class ReinstatePatientInQueue {

    private final IQueueEntryRepository queueEntryRepository;
    private final IQueueEventPublisher eventPublisher;

    private ReinstatePatientInQueue(IQueueEntryRepository queueEntryRepository,
                                     IQueueEventPublisher eventPublisher) {
        this.queueEntryRepository = queueEntryRepository;
        this.eventPublisher = eventPublisher;
    }

    public static ReinstatePatientInQueue create(IQueueEntryRepository queueEntryRepository,
                                                  IQueueEventPublisher eventPublisher) {
        return new ReinstatePatientInQueue(queueEntryRepository, eventPublisher);
    }

    public QueueEntry execute(UUID queueEntryId) {
        QueueEntry queueEntry = queueEntryRepository
                .findById(queueEntryId)
                .orElseThrow(() -> new QueueNotExistException("Não existe fila com esse ID"));

        if (!queueEntry.getQueueStatus().equals(EQueueStatus.AGENDADO)) {
            throw new QueueNotAllowedException("Não é possível reinserir a fila!");
        }

        queueEntry.reinstate();
        queueEntryRepository.save(queueEntry);
        eventPublisher.publishPatientReinstated(queueEntry);

        return queueEntry;
    }
}
