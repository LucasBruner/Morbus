package br.com.morbus.queueservice.domain.repository;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.QueueEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IQueueEntryRepository {
    void save(QueueEntry entry);
    Optional<QueueEntry> findById(UUID id);
    Optional<QueueEntry> findNextByPriority();
    Optional<QueueEntry> findByPatient(Patient patient);
    List<QueueEntry> findAllOrderedByPriority();
}
