package br.com.morbus.queueservice.domain.exception;

public class PatientHasActiveQueueEntriesException extends RuntimeException {
    public PatientHasActiveQueueEntriesException(String message) {
        super(message);
    }
}
