package br.com.morbus.queueservice.domain.exception;

public class PatientNotEligibleForProcedureException extends RuntimeException {
    public PatientNotEligibleForProcedureException(String message) {
        super(message);
    }
}
