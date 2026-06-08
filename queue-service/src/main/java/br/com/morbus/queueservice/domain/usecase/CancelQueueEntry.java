package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.domain.exception.QueueNotAllowedException;
import br.com.morbus.queueservice.domain.exception.QueueNotExistException;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.usecase.DTO.QueueCancelDTO;

public class CancelQueueEntry {
    private final IQueueEntryRepository queueEntryRepository;
    private final IQueueEventPublisher eventPublisher;

    private CancelQueueEntry(IQueueEntryRepository queueEntryRepository,
                             IQueueEventPublisher eventPublisher) {
        this.queueEntryRepository = queueEntryRepository;
        this.eventPublisher = eventPublisher;
    }

    public static CancelQueueEntry create(IQueueEntryRepository queueEntryRepository,
                                   IQueueEventPublisher eventPublisher) {
        return new CancelQueueEntry(queueEntryRepository, eventPublisher);
    }

    public QueueEntry run(QueueCancelDTO cancelDTO) {
        QueueEntry queueEntryCancel = queueEntryRepository
                .findById(cancelDTO.queueId())
                .orElseThrow(() -> new QueueNotExistException("Não existe fila com esse ID"));
        if (!queueEntryCancel.getQueueStatus().equals(EQueueStatus.AGUARDANDO) &&
                !queueEntryCancel.getQueueStatus().equals(EQueueStatus.AGENDADO)) {
            throw new QueueNotAllowedException("Não é possível cancelar a fila!");
        }
        queueEntryCancel.cancelQueue();
        queueEntryRepository.save(queueEntryCancel);
        eventPublisher.publishPatientCancelled(queueEntryCancel, cancelDTO.reason());
        return queueEntryCancel;
    }
}
