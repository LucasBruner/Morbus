package br.com.morbus.queueservice.domain.exception;

import java.util.UUID;

public class ProcedureNotFoundException extends RuntimeException {
    public ProcedureNotFoundException(UUID procedureId) {
        super("Procedimento não encontrado: " + procedureId);
    }
}
