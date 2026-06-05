package br.com.morbus.queueservice.domain.port;

import br.com.morbus.queueservice.domain.event.PatientRegisteredEvent;

public interface PatientRegisteredEventPublisher {
    void publish(PatientRegisteredEvent event);
}
