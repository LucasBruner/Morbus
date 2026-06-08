package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.domain.exception.QueueEmptyException;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;

public class CallNextPatient {

    private final IQueueEntryRepository queueEntryRepository;
    private final IQueueEventPublisher queueEventPublisher;
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
        queueEventPublisher.publishPatientCalled(queueEntryExistente);

        return queueEntryExistente;
    }
}
