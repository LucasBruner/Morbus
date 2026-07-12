package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.domain.exception.CampoObrigatorioException;
import br.com.morbus.regulacao.domain.exception.CotaExcedidaException;
import br.com.morbus.regulacao.domain.exception.IdPacienteIncorretoException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoPendenteException;
import br.com.morbus.regulacao.domain.exception.UnidadeSolicitanteDuplicadaException;
import br.com.morbus.regulacao.domain.exception.UnidadeSolicitanteNaoEncontradaException;
import br.com.morbus.regulacao.domain.exception.DuplicateSolicitacaoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TYPE_BASE = "https://morbus.sus.gov.br/problems/";

    @ExceptionHandler(DuplicateSolicitacaoException.class)
    public ProblemDetail handleDuplicate(DuplicateSolicitacaoException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(409);
        problem.setType(URI.create(TYPE_BASE + "solicitation-already-exists"));
        problem.setTitle("Conflito");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", e.getDefaultMessage()))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatus(400);
        problem.setType(URI.create(TYPE_BASE + "validation-error"));
        problem.setTitle("Dados inválidos");
        problem.setDetail("Um ou mais campos são inválidos");
        problem.setProperty("violations", violations);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(400);
        problem.setType(URI.create(TYPE_BASE + "invalid-request-body"));
        problem.setTitle("Requisição inválida");
        problem.setDetail("Corpo da requisição ausente ou malformado");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(403);
        problem.setType(URI.create(TYPE_BASE + "access-denied"));
        problem.setTitle("Acesso negado");
        problem.setDetail("Seu perfil não tem permissão para esta operação.");
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

    @ExceptionHandler(SolicitacaoNaoPendenteException.class)
    public ProblemDetail handleSolicitacaoNaoPendente(SolicitacaoNaoPendenteException e, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(422);
        problem.setType(URI.create(TYPE_BASE + "regulacao-not-allowed"));
        problem.setTitle("Solicitação não pendente");
        problem.setDetail(e.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(SolicitacaoNaoEncontradaException.class)
    public ProblemDetail handleSolicitacaoNotFound(SolicitacaoNaoEncontradaException e, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setType(URI.create(TYPE_BASE + "solicitation-not-found"));
        problem.setTitle("Solicitacao não encontrada");
        problem.setDetail(e.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(IdPacienteIncorretoException.class)
    public ProblemDetail handleIdPacienteIncorreto(IdPacienteIncorretoException e, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(403);
        problem.setTitle("Id incorreto");
        problem.setType(URI.create(TYPE_BASE + "id-paciente-incorreto"));
        problem.setDetail(e.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(CampoObrigatorioException.class)
    public ProblemDetail handleCampoObrigatorio(CampoObrigatorioException e, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(422);
        problem.setType(URI.create(TYPE_BASE + "missing-required-field"));
        problem.setTitle("Campo obrigatório ausente");
        problem.setDetail(e.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(CotaExcedidaException.class)
    public ProblemDetail handleCotaExcedida(CotaExcedidaException e, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(422);
        problem.setType(URI.create(TYPE_BASE + "quota-exceeded"));
        problem.setTitle("Cota excedida");
        problem.setDetail(e.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(UnidadeSolicitanteNaoEncontradaException.class)
    public ProblemDetail handleUnidadeSolicitanteNaoEncontrada(UnidadeSolicitanteNaoEncontradaException e, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setType(URI.create(TYPE_BASE + "unit-not-found"));
        problem.setTitle("Unidade solicitante não encontrada");
        problem.setDetail(e.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(UnidadeSolicitanteDuplicadaException.class)
    public ProblemDetail handleUnidadeSolicitanteDuplicada(UnidadeSolicitanteDuplicadaException e, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(409);
        problem.setType(URI.create(TYPE_BASE + "unit-already-exists"));
        problem.setTitle("Conflito");
        problem.setDetail(e.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ProblemDetail handleDateTimeParse(DateTimeParseException e, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(400);
        problem.setType(URI.create(TYPE_BASE + "invalid-request-body"));
        problem.setTitle("Dados inválidos");
        problem.setDetail("Parametro 'mes' deve estar no formato yyyy-MM");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
