package br.com.morbus.queueservice.domain.exception;

public class ProcedureNotFoundException extends RuntimeException {
    public ProcedureNotFoundException(Integer procedureId) {
        super("Procedimento não encontrado: " + procedureId);
    }
}
