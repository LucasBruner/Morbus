package br.com.morbus.queueservice.domain.repository;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.usecase.RegisterPatientCommand;

import java.util.List;
import java.util.Optional;

public interface IQueueEntryRepository {
    void save(QueueEntry queueEntry);
    QueueEntry execute(RegisterPatientCommand command);
    Optional<QueueEntry> findById(Integer id);
    QueueEntry findNextByPriority();
    List<QueueEntry> findAllOrderedByPriority();
}
