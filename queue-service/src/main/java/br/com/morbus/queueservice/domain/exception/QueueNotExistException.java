package br.com.morbus.queueservice.domain.exception;

public class QueueNotExistException extends RuntimeException {
    public QueueNotExistException(String e) {
        super(e);
    }
}
