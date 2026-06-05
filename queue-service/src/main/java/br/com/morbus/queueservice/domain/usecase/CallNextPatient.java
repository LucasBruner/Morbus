package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.domain.exceptions.QueueEmptyException;

import java.time.LocalDateTime;

public class CallNextPatient {

    IQueueEntryRepository queueEntryRepository;
    IQueueEventPublisher queueEventPublisher;
    private CallNextPatient(IQueueEntryRepository queueEntryRepository,
                            IQueueEventPublisher queueEventPublisher) {
        this.queueEntryRepository = queueEntryRepository;
        this.queueEventPublisher = queueEventPublisher;
    }

    public static CallNextPatient create(IQueueEntryRepository queueEntryRepository,
                                         IQueueEventPublisher queueEventPublisher) {
        return new CallNextPatient(queueEntryRepository, queueEventPublisher);
    }

    public QueueEntry run() {
        QueueEntry queueEntryExistente = queueEntryRepository
                .findNextByPriority()
                .orElseThrow(() -> new QueueEmptyException("Não há pacientes na fila"));

        queueEntryExistente.call();

        queueEntryRepository.save(queueEntryExistente);
        queueEventPublisher.publish(queueEntryExistente);

        return queueEntryExistente;
    }
}
