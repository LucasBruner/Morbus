package br.com.morbus.queueservice.domain.exceptions;

public class QueueNotAllowedException extends RuntimeException {
    public QueueNotAllowedException(String e) {
        super(e);
    }
}
