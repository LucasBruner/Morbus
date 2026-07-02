package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.domain.exception.CampoObrigatorioException;
import br.com.morbus.regulacao.domain.exception.CotaExcedidaException;
import br.com.morbus.regulacao.domain.exception.IdPacienteIncorretoException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoPendenteException;
import br.com.morbus.regulacao.domain.exception.UnidadeSolicitanteDuplicadaException;
import br.com.morbus.regulacao.domain.exception.UnidadeSolicitanteNaoEncontradaException;
import org.springframework.http.ProblemDetail;
import br.com.morbus.regulacao.domain.exception.DuplicateSolicitacaoException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.format.DateTimeParseException;
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

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(403);
        problem.setType(URI.create("https://httpstatuses.com/403"));
        problem.setTitle("Acesso negado");
        problem.setDetail("Seu perfil não tem permissão para esta operação.");
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

    @ExceptionHandler(SolicitacaoNaoPendenteException.class)
    public ProblemDetail handleSolicitacaoNaoPendente(SolicitacaoNaoPendenteException e) {
        ProblemDetail problem = ProblemDetail.forStatus(422);
        problem.setType(URI.create("https://httpstatuses.com/regulacao-not-allowed"));
        problem.setTitle("Solicitação não pendente");
        problem.setDetail(e.getMessage());
        return problem;
    }

    @ExceptionHandler(SolicitacaoNaoEncontradaException.class)
    public ProblemDetail handleSolicitacaoNotFound(SolicitacaoNaoEncontradaException e) {
        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setType(URI.create("https://httpstatuses.com/solicitacao-not-found"));
        problem.setTitle("Solicitacao não encontrada");
        problem.setDetail(e.getMessage());
        return problem;
    }

    @ExceptionHandler(IdPacienteIncorretoException.class)
    public ProblemDetail handleIdPacienteIncorreto(IdPacienteIncorretoException e) {
        ProblemDetail problem = ProblemDetail.forStatus(403);
        problem.setTitle("Id incorreto");
        problem.setType(URI.create("https://httpstatuses.com/403"));
        problem.setDetail(e.getMessage());
        return problem;
    }

    @ExceptionHandler(CampoObrigatorioException.class)
    public ProblemDetail handleCampoObrigatorio(CampoObrigatorioException e) {
        ProblemDetail problem = ProblemDetail.forStatus(400);
        problem.setType(URI.create("https://httpstatuses.com/400"));
        problem.setTitle("Dados inválidos");
        problem.setDetail(e.getMessage());
        return problem;
    }

    @ExceptionHandler(CotaExcedidaException.class)
    public ProblemDetail handleCotaExcedida(CotaExcedidaException e) {
        ProblemDetail problem = ProblemDetail.forStatus(422);
        problem.setType(URI.create("https://morbus.sus.gov.br/problems/quota-exceeded"));
        problem.setTitle("Cota excedida");
        problem.setDetail(e.getMessage());
        return problem;
    }

    @ExceptionHandler(UnidadeSolicitanteNaoEncontradaException.class)
    public ProblemDetail handleUnidadeSolicitanteNaoEncontrada(UnidadeSolicitanteNaoEncontradaException e) {
        ProblemDetail problem = ProblemDetail.forStatus(404);
        problem.setType(URI.create("https://httpstatuses.com/404"));
        problem.setTitle("Unidade solicitante não encontrada");
        problem.setDetail(e.getMessage());
        return problem;
    }

    @ExceptionHandler(UnidadeSolicitanteDuplicadaException.class)
    public ProblemDetail handleUnidadeSolicitanteDuplicada(UnidadeSolicitanteDuplicadaException e) {
        ProblemDetail problem = ProblemDetail.forStatus(409);
        problem.setType(URI.create("https://httpstatuses.com/409"));
        problem.setTitle("Conflito");
        problem.setDetail(e.getMessage());
        return problem;
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ProblemDetail handleDateTimeParse(DateTimeParseException e) {
        ProblemDetail problem = ProblemDetail.forStatus(400);
        problem.setType(URI.create("https://httpstatuses.com/400"));
        problem.setTitle("Dados inválidos");
        problem.setDetail("Parametro 'mes' deve estar no formato yyyy-MM");
        return problem;
    }
}
