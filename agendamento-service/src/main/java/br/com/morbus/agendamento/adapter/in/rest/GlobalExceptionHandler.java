package br.com.morbus.agendamento.adapter.in.rest;

import br.com.morbus.agendamento.domain.exception.*;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateAgendamentoException.class)
    public ProblemDetail handleDuplicate(DuplicateAgendamentoException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(409);
        problem.setType(URI.create("https://httpstatuses.com/409"));
        problem.setTitle("Conflito");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(DuplicateScheduleException.class)
    public ProblemDetail handleDuplicateSchedule(DuplicateScheduleException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(409);
        problem.setType(URI.create("https://httpstatuses.com/409"));
        problem.setTitle("Conflito");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(InvalidSchedulePeriodException.class)
    public ProblemDetail handleInvalidSchedulePeriod(InvalidSchedulePeriodException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(422);
        problem.setType(URI.create("https://httpstatuses.com/422"));
        problem.setTitle("Grade invalida");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(SlotNotFoundException.class)
    public ProblemDetail handleSlotNotFound(SlotNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setType(URI.create("https://httpstatuses.com/404"));
        problem.setTitle("Slot nao encontrado");
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
        problem.setTitle("Dados invalidos");
        problem.setDetail(detail);
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(403);
        problem.setType(URI.create("https://httpstatuses.com/403"));
        problem.setTitle("Acesso negado");
        problem.setDetail("Seu perfil nao tem permissão para esta operação.");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(500);
        problem.setType(URI.create("https://httpstatuses.com/500"));
        problem.setTitle("Erro interno");
        problem.setDetail("Ocorreu um erro inesperado. Tente novamente mais tarde.");
        return problem;
    }
}
