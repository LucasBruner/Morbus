package br.com.morbus.regulacao.domain.exception;

public class SolicitacaoNaoPendenteException extends RuntimeException {
    public SolicitacaoNaoPendenteException(String message) {
        super(message);
    }
}
