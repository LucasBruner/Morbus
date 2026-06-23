package br.com.morbus.regulacao.domain.exception;

public class DuplicateSolicitacaoException extends RuntimeException {
    public DuplicateSolicitacaoException(String message) {
        super(message);
    }
}
