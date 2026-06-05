package br.com.morbus.queueservice.domain.event;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.QueueEntry;

public class PatientRegisteredEvent {
    public static final String ROUTING_KEY = "patient.registered";

    private final Patient patient;
    private final QueueEntry queueEntry;

    public PatientRegisteredEvent(Patient patient, QueueEntry queueEntry) {
        this.patient = patient;
        this.queueEntry = queueEntry;
    }

    public Patient getPatient() {
        return patient;
    }

    public QueueEntry getQueueEntry() {
        return queueEntry;
    }
}
