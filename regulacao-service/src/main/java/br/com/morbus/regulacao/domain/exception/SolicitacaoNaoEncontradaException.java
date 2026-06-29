package br.com.morbus.regulacao.domain.exception;

public class SolicitacaoNaoEncontradaException extends RuntimeException {
    public SolicitacaoNaoEncontradaException(String message) {
        super(message);
    }
}
