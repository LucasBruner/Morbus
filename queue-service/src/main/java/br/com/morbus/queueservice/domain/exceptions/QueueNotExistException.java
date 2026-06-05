package br.com.morbus.queueservice.domain.exceptions;

public class QueueNotExistException extends RuntimeException {
    public QueueNotExistException(String e) {
        super(e);
    }
}
