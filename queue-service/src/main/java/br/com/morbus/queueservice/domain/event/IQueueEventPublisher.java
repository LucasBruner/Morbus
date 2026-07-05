package br.com.morbus.queueservice.domain.event;

import br.com.morbus.queueservice.domain.entity.QueueEntry;

public interface IQueueEventPublisher {
    void publishPatientRegistered(QueueEntry queueEntry);
    void publishPatientCalled(QueueEntry queueEntry);
    void publishPriorityUpdated(QueueEntry queueEntry);
    void publishPatientCancelled(QueueEntry queueEntry, String reason);
    void publishPatientReinstated(QueueEntry queueEntry);
}
