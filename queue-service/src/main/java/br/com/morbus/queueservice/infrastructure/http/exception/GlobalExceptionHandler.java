package br.com.morbus.queueservice.infrastructure.http.exception;

import br.com.morbus.queueservice.domain.exception.ActiveQueueEntriesExistException;
import br.com.morbus.queueservice.domain.exception.PatientAgeNotEligibleException;
import br.com.morbus.queueservice.domain.exception.PatientAlreadyExistsException;
import br.com.morbus.queueservice.domain.exception.PatientAlreadyInactiveException;
import br.com.morbus.queueservice.domain.exception.PatientAlreadyRegisteredException;
import br.com.morbus.queueservice.domain.exception.PatientHasActiveQueueEntriesException;
import br.com.morbus.queueservice.domain.exception.PatientInactiveException;
import br.com.morbus.queueservice.domain.exception.PatientNotEligibleForProcedureException;
import br.com.morbus.queueservice.domain.exception.PatientNotFoundException;
import br.com.morbus.queueservice.domain.exception.ProcedureAlreadyAssignedException;
import br.com.morbus.queueservice.domain.exception.ProcedureNotAssignedException;
import br.com.morbus.queueservice.domain.exception.ProcedureNotFoundException;
import br.com.morbus.queueservice.domain.exception.QueueEmptyException;
import br.com.morbus.queueservice.domain.exception.QueueNotAllowedException;
import br.com.morbus.queueservice.domain.exception.QueueNotExistException;
import br.com.morbus.queueservice.domain.exception.QuotaExceededException;
import br.com.morbus.queueservice.domain.exception.QuotaNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TYPE_BASE = "https://morbus.sus.gov.br/problems/";

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<ProblemDetail> handlePatientNotFound(
            PatientNotFoundException ex, HttpServletRequest request) {
        return notFound("patient-not-found", "Paciente não encontrado", ex, request);
    }

    @ExceptionHandler(ProcedureNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleProcedureNotFound(
            ProcedureNotFoundException ex, HttpServletRequest request) {
        return notFound("procedure-not-found", "Procedimento não encontrado", ex, request);
    }

    @ExceptionHandler(QueueNotExistException.class)
    public ResponseEntity<ProblemDetail> handleQueueNotExist(
            QueueNotExistException ex, HttpServletRequest request) {
        return notFound("queue-not-found", "Entrada de fila não encontrada", ex, request);
    }

    @ExceptionHandler(QueueEmptyException.class)
    public ResponseEntity<ProblemDetail> handleQueueEmpty(
            QueueEmptyException ex, HttpServletRequest request) {
        return notFound("queue-empty", "Fila vazia", ex, request);
    }

    @ExceptionHandler(PatientAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handlePatientAlreadyExists(
            PatientAlreadyExistsException ex, HttpServletRequest request) {
        return conflict("patient-already-exists", ex, request);
    }

    @ExceptionHandler(PatientAlreadyRegisteredException.class)
    public ResponseEntity<ProblemDetail> handlePatientAlreadyRegistered(
            PatientAlreadyRegisteredException ex, HttpServletRequest request) {
        return conflict("patient-already-registered", ex, request);
    }

    @ExceptionHandler(ActiveQueueEntriesExistException.class)
    public ResponseEntity<ProblemDetail> handleActiveQueueEntriesExist(
            ActiveQueueEntriesExistException ex, HttpServletRequest request) {
        return conflict("active-queue-entries-exist", ex, request);
    }

    @ExceptionHandler(ProcedureAlreadyAssignedException.class)
    public ResponseEntity<ProblemDetail> handleProcedureAlreadyAssigned(
            ProcedureAlreadyAssignedException ex, HttpServletRequest request) {
        return conflict("procedure-already-assigned", ex, request);
    }

    @ExceptionHandler(QueueNotAllowedException.class)
    public ResponseEntity<ProblemDetail> handleQueueNotAllowed(
            QueueNotAllowedException ex, HttpServletRequest request) {
        return unprocessable("queue-not-allowed", "Operação não permitida na fila", ex, request);
    }

    @ExceptionHandler(PatientAgeNotEligibleException.class)
    public ResponseEntity<ProblemDetail> handlePatientAgeNotEligible(
            PatientAgeNotEligibleException ex, HttpServletRequest request) {
        return unprocessable("age-not-eligible", "Paciente fora da faixa etária permitida", ex, request);
    }

    @ExceptionHandler(PatientNotEligibleForProcedureException.class)
    public ResponseEntity<ProblemDetail> handlePatientNotEligibleForProcedure(
            PatientNotEligibleForProcedureException ex, HttpServletRequest request) {
        return unprocessable("patient-not-eligible", "Paciente não elegível para o procedimento", ex, request);
    }

    @ExceptionHandler(PatientAlreadyInactiveException.class)
    public ResponseEntity<ProblemDetail> handlePatientAlreadyInactive(
            PatientAlreadyInactiveException ex, HttpServletRequest request) {
        return unprocessable("patient-already-inactive", "Paciente já está inativo", ex, request);
    }

    @ExceptionHandler(PatientHasActiveQueueEntriesException.class)
    public ResponseEntity<ProblemDetail> handlePatientHasActiveQueueEntries(
            PatientHasActiveQueueEntriesException ex, HttpServletRequest request) {
        return unprocessable("patient-has-active-queue-entries", "Paciente possui entradas ativas na fila", ex, request);
    }

    @ExceptionHandler(PatientInactiveException.class)
    public ResponseEntity<ProblemDetail> handlePatientInactive(
            PatientInactiveException ex, HttpServletRequest request) {
        return unprocessable("patient-inactive", "Paciente inativo", ex, request);
    }

    @ExceptionHandler(ProcedureNotAssignedException.class)
    public ResponseEntity<ProblemDetail> handleProcedureNotAssigned(
            ProcedureNotAssignedException ex, HttpServletRequest request) {
        return unprocessable("procedure-not-assigned", "Procedimento não atribuído ao paciente", ex, request);
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ProblemDetail> handleQuotaExceeded(
            QuotaExceededException ex, HttpServletRequest request) {
        return conflict("quota-exceeded", ex, request);
    }

    @ExceptionHandler(QuotaNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleQuotaNotFound(
            QuotaNotFoundException ex, HttpServletRequest request) {
        return notFound("quota-not-found", "Cota não encontrada", ex, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        List<Map<String, String>> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of("field", err.getField(), "message", err.getDefaultMessage()))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos");
        problem.setType(URI.create(TYPE_BASE + "validation-error"));
        problem.setTitle("Requisição inválida");
        problem.setProperty("violations", violations);

        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou malformado");
        problem.setType(URI.create(TYPE_BASE + "invalid-request-body"));
        problem.setTitle("Requisição inválida");

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(
            Exception ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado. Tente novamente.");
        problem.setType(URI.create(TYPE_BASE + "internal-error"));
        problem.setTitle("Erro interno");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ResponseEntity<ProblemDetail> notFound(
            String slug, String title, RuntimeException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + slug));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    private ResponseEntity<ProblemDetail> conflict(
            String slug, RuntimeException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + slug));
        problem.setTitle("Conflito de dados");
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    private ResponseEntity<ProblemDetail> unprocessable(
            String slug, String title, RuntimeException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create(TYPE_BASE + slug));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }
}
