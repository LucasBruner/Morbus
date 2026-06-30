package br.com.morbus.agendamento.domain.exception;

public class InvalidSchedulePeriodException extends RuntimeException {

    public InvalidSchedulePeriodException(String message) {
        super(message);
    }
}
