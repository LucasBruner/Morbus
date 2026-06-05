package br.com.morbus.queueservice.domain.exceptions;

public class QueueEmptyException extends RuntimeException {
    public QueueEmptyException(String e) {
        super(e);
    }
}
