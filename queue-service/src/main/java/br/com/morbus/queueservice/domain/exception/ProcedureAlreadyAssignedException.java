package br.com.morbus.queueservice.domain.exception;

public class ProcedureAlreadyAssignedException extends RuntimeException {
    public ProcedureAlreadyAssignedException(String message) {
        super(message);
    }
}
