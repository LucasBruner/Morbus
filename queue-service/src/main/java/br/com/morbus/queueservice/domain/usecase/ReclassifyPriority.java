package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.domain.exceptions.QueueNotAllowedException;
import br.com.morbus.queueservice.domain.exceptions.QueueNotExistException;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.usecase.DTO.QueueEntryRiskQueuePosition;
import br.com.morbus.queueservice.domain.usecase.DTO.QueueUpdateRiskColorDTO;

public class ReclassifyPriority {
    private final IQueueEntryRepository queueEntryRepository;
    private final IQueueEventPublisher eventPublisher;

    private ReclassifyPriority(IQueueEventPublisher eventPublisher,
                               IQueueEntryRepository queueEntryRepository) {
        this.eventPublisher = eventPublisher;
        this.queueEntryRepository = queueEntryRepository;
    }

    public static ReclassifyPriority create(IQueueEventPublisher eventPublisher,
                                            IQueueEntryRepository queueEntryRepository) {
        return new ReclassifyPriority(eventPublisher, queueEntryRepository);
    }

    public QueueEntryRiskQueuePosition run(QueueUpdateRiskColorDTO queueUpdateRiskColorDTO) {
        QueueEntry queueEntryUpdateRiskyColor = queueEntryRepository
                .findById(queueUpdateRiskColorDTO.queueId())
                .orElseThrow(() -> new QueueNotExistException("Não existe fila com esse ID"));

        if(queueEntryUpdateRiskyColor.getQueueStatus().equals(EQueueStatus.AGUARDANDO)
                || queueEntryUpdateRiskyColor.getQueueStatus().equals(EQueueStatus.DEVOLVIDO)) {
            queueEntryUpdateRiskyColor.updateRiskColor(queueUpdateRiskColorDTO.riskColor());
            queueEntryRepository.save(queueEntryUpdateRiskyColor);
        } else {
            throw new QueueNotAllowedException("Não é possível reclassificar o paciente!");
        }
        eventPublisher.publishPriorityUpdated(queueEntryUpdateRiskyColor);
        int posicaoPatientQueue = queueEntryRepository.countEntriesWithHigherPriority(queueEntryUpdateRiskyColor);
        return new QueueEntryRiskQueuePosition(queueEntryUpdateRiskyColor, posicaoPatientQueue);
    }
}
