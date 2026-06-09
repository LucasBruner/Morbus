package br.com.morbus.queueservice.domain.exception;

public class PatientAlreadyInactiveException extends RuntimeException {
    public PatientAlreadyInactiveException(String e) {
        super(e);
    }
}
