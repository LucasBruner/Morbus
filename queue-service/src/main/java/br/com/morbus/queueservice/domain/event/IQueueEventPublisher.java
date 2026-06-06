package br.com.morbus.queueservice.domain.event;

import br.com.morbus.queueservice.domain.entity.QueueEntry;

public interface IQueueEventPublisher {
    void publish(QueueEntry queueEntry);
    void update (QueueEntry queueEntry);
    void cancel (QueueEntry queueEntry, String reason);
}
