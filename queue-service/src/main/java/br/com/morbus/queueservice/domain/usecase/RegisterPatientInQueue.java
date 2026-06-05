package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.QueueEntry;

public interface RegisterPatientInQueue {
    QueueEntry execute(RegisterPatientCommand command);
}
