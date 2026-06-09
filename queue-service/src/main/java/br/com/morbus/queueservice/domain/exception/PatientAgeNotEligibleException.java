package br.com.morbus.queueservice.domain.exception;

public class PatientAgeNotEligibleException extends RuntimeException {
    public PatientAgeNotEligibleException(String message) {
        super(message);
    }
}
