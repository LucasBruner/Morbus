package br.com.morbus.queueservice.domain.repository;

import br.com.morbus.queueservice.domain.entity.QueueEntry;

import java.util.List;
import java.util.Optional;

public interface IQueueEntryRepository {
    void save();
    Optional<QueueEntry> findById(Integer id);
    QueueEntry findNextByPriority();
    List<QueueEntry> findAllOrderedByPriority();
}
