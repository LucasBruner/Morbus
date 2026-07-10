package br.com.morbus.agendamento.domain.exception;

public class SlotIndisponivelException extends RuntimeException {
    public SlotIndisponivelException(String message) {
        super(message);
    }
}
