package br.com.morbus.agendamento.domain.exception;

public class InvalidAgendamentoStatusException extends RuntimeException {
    public InvalidAgendamentoStatusException(String message) {
        super(message);
    }
}
