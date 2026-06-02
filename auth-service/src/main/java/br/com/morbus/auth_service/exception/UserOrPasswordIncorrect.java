package br.com.fiap.auth_service.exception;

public class UserOrPasswordIncorrect extends RuntimeException {
    public UserOrPasswordIncorrect(String e) {
        super(e);
    }
}
