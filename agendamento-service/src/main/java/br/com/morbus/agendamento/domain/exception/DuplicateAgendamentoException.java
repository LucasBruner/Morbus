package br.com.morbus.agendamento.domain.exception;

public class DuplicateAgendamentoException extends RuntimeException {

    public DuplicateAgendamentoException(String message) {
        super(message);
    }
}
