package br.com.morbus.agendamento.domain.exception;

public class DuplicateScheduleException extends RuntimeException {

    public DuplicateScheduleException(String message) {
        super(message);
    }
}
