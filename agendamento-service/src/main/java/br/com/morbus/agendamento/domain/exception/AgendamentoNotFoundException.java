package br.com.morbus.agendamento.domain.exception;

public class AgendamentoNotFoundException extends RuntimeException {
    public AgendamentoNotFoundException(String message) {
        super(message);
    }
}
