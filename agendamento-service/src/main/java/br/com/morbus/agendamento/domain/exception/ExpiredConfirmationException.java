package br.com.morbus.agendamento.domain.exception;

public class ExpiredConfirmationException extends RuntimeException {
    public ExpiredConfirmationException(String message) {
        super(message);
    }
}
