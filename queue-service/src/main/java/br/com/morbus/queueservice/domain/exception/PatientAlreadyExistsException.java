package br.com.morbus.queueservice.domain.exception;

public class PatientAlreadyExistsException extends RuntimeException {
    public PatientAlreadyExistsException(String e) {
        super(e);
    }
}
