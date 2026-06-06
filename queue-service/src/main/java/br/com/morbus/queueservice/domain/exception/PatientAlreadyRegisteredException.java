package br.com.morbus.queueservice.domain.exception;

public class PatientAlreadyRegisteredException extends RuntimeException {
    public PatientAlreadyRegisteredException(String e) {
        super(e);
    }
}
