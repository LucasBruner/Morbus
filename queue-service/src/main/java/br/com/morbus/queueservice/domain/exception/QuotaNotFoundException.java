package br.com.morbus.queueservice.domain.exception;

public class QuotaNotFoundException extends RuntimeException {
    public QuotaNotFoundException(String message) {
        super(message);
    }
}
