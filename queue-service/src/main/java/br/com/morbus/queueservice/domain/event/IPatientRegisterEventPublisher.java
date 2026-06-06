package br.com.morbus.queueservice.domain.event;

import br.com.morbus.queueservice.domain.entity.Patient;

public interface IPatientRegisterEventPublisher {
    void publish(Patient patient);
    void publishGrupoLegalUpdated(Patient patient);
}
