package br.com.morbus.authservice.exception;

public class UserOrPasswordIncorrect extends RuntimeException {
    public UserOrPasswordIncorrect(String e) {
        super(e);
    }
}
