package br.com.morbus.queueservice.domain.exception;

public class ActiveQueueEntriesExistException extends RuntimeException {
    public ActiveQueueEntriesExistException(String message) {
        super(message);
    }
}
