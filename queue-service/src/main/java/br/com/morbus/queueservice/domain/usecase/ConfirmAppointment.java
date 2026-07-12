package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.exception.QueueNotAllowedException;
import br.com.morbus.queueservice.domain.exception.QueueNotExistException;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;

import java.util.UUID;

public class ConfirmAppointment {

    private final IQueueEntryRepository queueEntryRepository;

    private ConfirmAppointment(IQueueEntryRepository queueEntryRepository) {
        this.queueEntryRepository = queueEntryRepository;
    }

    public static ConfirmAppointment create(IQueueEntryRepository queueEntryRepository) {
        return new ConfirmAppointment(queueEntryRepository);
    }

    public QueueEntry execute(UUID queueEntryId) {
        QueueEntry queueEntry = queueEntryRepository
                .findById(queueEntryId)
                .orElseThrow(() -> new QueueNotExistException("Não existe fila com esse ID"));

        if (!queueEntry.getQueueStatus().equals(EQueueStatus.CHAMADO)) {
            throw new QueueNotAllowedException("Não é possível confirmar agendamento para esta entrada");
        }

        queueEntry.confirmAppointment();
        queueEntryRepository.save(queueEntry);

        return queueEntry;
    }
}
