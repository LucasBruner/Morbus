package br.com.morbus.regulacao.adapters.in.rest;

import org.springframework.http.ProblemDetail;
import br.com.morbus.regulacao.domain.exception.DuplicateSolicitacaoException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateSolicitacaoException.class)
    public ProblemDetail handleDuplicate(DuplicateSolicitacaoException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(409);
        problem.setType(URI.create("https://httpstatuses.com/409"));
        problem.setTitle("Conflito");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ProblemDetail problem = ProblemDetail.forStatus(400);
        problem.setType(URI.create("https://httpstatuses.com/400"));
        problem.setTitle("Dados inválidos");
        problem.setDetail(detail);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(500);
        problem.setType(URI.create("https://httpstatuses.com/500"));
        problem.setTitle("Erro interno");
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
