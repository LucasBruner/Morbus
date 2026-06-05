package br.com.morbus.queueservice.domain.event;

import br.com.morbus.queueservice.domain.entity.QueueEntry;

public interface IQueueEventPublisher {
    void publish(QueueEntry queueEntryExistente);
}
