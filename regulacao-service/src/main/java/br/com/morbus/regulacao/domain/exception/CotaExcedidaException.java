package br.com.morbus.regulacao.domain.exception;

public class CotaExcedidaException extends RuntimeException {
    public CotaExcedidaException(String message) {
        super(message);
    }
}
