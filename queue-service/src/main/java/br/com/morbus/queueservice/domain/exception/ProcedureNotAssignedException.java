package br.com.morbus.queueservice.domain.exception;

public class ProcedureNotAssignedException extends RuntimeException {
    public ProcedureNotAssignedException(String message) {
        super(message);
    }
}
