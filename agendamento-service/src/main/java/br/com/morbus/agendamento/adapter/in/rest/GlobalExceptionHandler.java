package br.com.morbus.agendamento.adapter.in.rest;

import br.com.morbus.agendamento.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TYPE_BASE = "https://morbus.sus.gov.br/problems/";

    @ExceptionHandler(DuplicateAgendamentoException.class)
    public ProblemDetail handleDuplicate(DuplicateAgendamentoException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(409);
        problem.setType(URI.create(TYPE_BASE + "appointment-already-exists"));
        problem.setTitle("Conflito");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(DuplicateScheduleException.class)
    public ProblemDetail handleDuplicateSchedule(DuplicateScheduleException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(409);
        problem.setType(URI.create(TYPE_BASE + "schedule-already-exists"));
        problem.setTitle("Conflito");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(InvalidSchedulePeriodException.class)
    public ProblemDetail handleInvalidSchedulePeriod(InvalidSchedulePeriodException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(422);
        problem.setType(URI.create(TYPE_BASE + "invalid-schedule-period"));
        problem.setTitle("Grade invalida");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(ExpiredConfirmationException.class)
    public ProblemDetail handleExpiredConfirmation(ExpiredConfirmationException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(422);
        problem.setType(URI.create(TYPE_BASE + "expired-confirmation"));
        problem.setTitle("Confirmacao expirada");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(InvalidAgendamentoStatusException.class)
    public ProblemDetail handleInvalidStatus(InvalidAgendamentoStatusException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(422);
        problem.setType(URI.create(TYPE_BASE + "appointment-not-allowed"));
        problem.setTitle("Status invalido");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(AgendamentoNotFoundException.class)
    public ProblemDetail handleAgendamentoNotFound(AgendamentoNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setType(URI.create(TYPE_BASE + "appointment-not-found"));
        problem.setTitle("Agendamento nao encontrado");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(ScheduleNotFoundException.class)
    public ProblemDetail handleScheduleNotFound(ScheduleNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setType(URI.create(TYPE_BASE + "schedule-not-found"));
        problem.setTitle("Schedule nao encontrado");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(CancelamentoNaoPermitidoException.class)
    public ProblemDetail handleCancelamentoNaoPermitido(CancelamentoNaoPermitidoException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(422);
        problem.setType(URI.create(TYPE_BASE + "cancel-not-allowed"));
        problem.setTitle("Cancelamento nao permitido");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(SlotNotFoundException.class)
    public ProblemDetail handleSlotNotFound(SlotNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setType(URI.create(TYPE_BASE + "slot-not-found"));
        problem.setTitle("Slot nao encontrado");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(SlotIndisponivelException.class)
    public ProblemDetail handleSlotIndisponivel(SlotIndisponivelException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(422);
        problem.setType(URI.create(TYPE_BASE + "slot-unavailable"));
        problem.setTitle("Slot indisponivel");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of("field", err.getField(), "message", String.valueOf(err.getDefaultMessage())))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatus(400);
        problem.setType(URI.create(TYPE_BASE + "validation-error"));
        problem.setTitle("Dados invalidos");
        problem.setDetail("Um ou mais campos sao invalidos");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("violations", violations);
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(403);
        problem.setType(URI.create(TYPE_BASE + "access-denied"));
        problem.setTitle("Acesso negado");
        problem.setDetail("Seu perfil nao tem permissão para esta operação.");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(500);
        problem.setType(URI.create(TYPE_BASE + "internal-error"));
        problem.setTitle("Erro interno");
        problem.setDetail("Ocorreu um erro inesperado. Tente novamente mais tarde.");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
