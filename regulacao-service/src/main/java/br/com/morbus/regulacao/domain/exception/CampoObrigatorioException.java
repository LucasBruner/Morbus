package br.com.morbus.regulacao.domain.exception;

public class CampoObrigatorioException extends RuntimeException {
    public CampoObrigatorioException(String message) {
        super(message);
    }
}
