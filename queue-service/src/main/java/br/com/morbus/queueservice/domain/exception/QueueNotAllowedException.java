package br.com.morbus.queueservice.domain.exception;

public class QueueNotAllowedException extends RuntimeException {
    public QueueNotAllowedException(String e) {
        super(e);
    }
}
