package br.com.morbus.queueservice.domain.exception;

public class QueueEmptyException extends RuntimeException {
    public QueueEmptyException(String e) {
        super(e);
    }
}
