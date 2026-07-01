package br.com.morbus.agendamento.domain.exception;

public class InvalidSlotStatusException extends RuntimeException {

    public InvalidSlotStatusException(String message) {
        super(message);
    }
}
