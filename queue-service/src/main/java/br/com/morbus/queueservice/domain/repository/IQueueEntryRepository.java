package br.com.morbus.queueservice.domain.repository;

import br.com.morbus.queueservice.domain.entity.QueueEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IQueueEntryRepository {
    void save(QueueEntry entry);
    Optional<QueueEntry> findById(UUID id);
    Optional<QueueEntry> findNextByPriority();
    List<QueueEntry> findAllOrderedByPriority();
    int countEntriesWithHigherPriority(QueueEntry entry); // Conta quantos têm score menor (maior prioridade) que este paciente
}
