package br.com.morbus.queueservice.infrastructure.http.exception;

import br.com.morbus.queueservice.domain.exception.ActiveQueueEntriesExistException;
import br.com.morbus.queueservice.domain.exception.PatientAgeNotEligibleException;
import br.com.morbus.queueservice.domain.exception.PatientAlreadyExistsException;
import br.com.morbus.queueservice.domain.exception.PatientAlreadyInactiveException;
import br.com.morbus.queueservice.domain.exception.PatientAlreadyRegisteredException;
import br.com.morbus.queueservice.domain.exception.PatientInactiveException;
import br.com.morbus.queueservice.domain.exception.PatientNotEligibleForProcedureException;
import br.com.morbus.queueservice.domain.exception.PatientNotFoundException;
import br.com.morbus.queueservice.domain.exception.ProcedureAlreadyAssignedException;
import br.com.morbus.queueservice.domain.exception.ProcedureNotAssignedException;
import br.com.morbus.queueservice.domain.exception.ProcedureNotFoundException;
import br.com.morbus.queueservice.domain.exception.QueueEmptyException;
import br.com.morbus.queueservice.domain.exception.QueueNotAllowedException;
import br.com.morbus.queueservice.domain.exception.QueueNotExistException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private static final String TYPE_BASE = "https://morbus.sus.gov.br/problems/";
    private static final String INSTANCE_URI = "/api/v1/test";

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(INSTANCE_URI);
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private void assertProblemDetail(ProblemDetail body, HttpStatus expectedStatus,
                                     String expectedTypeSlug, String expectedDetail) {
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(expectedStatus.value());
        assertThat(body.getType().toString()).isEqualTo(TYPE_BASE + expectedTypeSlug);
        assertThat(body.getDetail()).isEqualTo(expectedDetail);
        assertThat(body.getInstance().toString()).isEqualTo(INSTANCE_URI);
    }

    // ── 404 Not Found ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("404 Not Found")
    class NotFound {

        @Test
        @DisplayName("PatientNotFoundException → 404 patient-not-found")
        void patientNotFound() {
            var ex = new PatientNotFoundException("Paciente não encontrado");
            ResponseEntity<ProblemDetail> response = handler.handlePatientNotFound(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertProblemDetail(response.getBody(), HttpStatus.NOT_FOUND,
                    "patient-not-found", "Paciente não encontrado");
            assertThat(response.getBody().getTitle()).isEqualTo("Paciente não encontrado");
        }

        @Test
        @DisplayName("ProcedureNotFoundException → 404 procedure-not-found")
        void procedureNotFound() {
            UUID id = UUID.randomUUID();
            var ex = new ProcedureNotFoundException(id);
            ResponseEntity<ProblemDetail> response = handler.handleProcedureNotFound(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getType().toString()).isEqualTo(TYPE_BASE + "procedure-not-found");
            assertThat(response.getBody().getDetail()).contains(id.toString());
        }

        @Test
        @DisplayName("QueueNotExistException → 404 queue-not-found")
        void queueNotExist() {
            var ex = new QueueNotExistException("Entrada de fila não existe");
            ResponseEntity<ProblemDetail> response = handler.handleQueueNotExist(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertProblemDetail(response.getBody(), HttpStatus.NOT_FOUND,
                    "queue-not-found", "Entrada de fila não existe");
        }

        @Test
        @DisplayName("QueueEmptyException → 404 queue-empty")
        void queueEmpty() {
            var ex = new QueueEmptyException("Fila está vazia");
            ResponseEntity<ProblemDetail> response = handler.handleQueueEmpty(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertProblemDetail(response.getBody(), HttpStatus.NOT_FOUND,
                    "queue-empty", "Fila está vazia");
        }
    }

    // ── 409 Conflict ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("409 Conflict")
    class Conflict {

        @Test
        @DisplayName("PatientAlreadyExistsException → 409 patient-already-exists")
        void patientAlreadyExists() {
            var ex = new PatientAlreadyExistsException("CPF já cadastrado");
            ResponseEntity<ProblemDetail> response = handler.handlePatientAlreadyExists(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertProblemDetail(response.getBody(), HttpStatus.CONFLICT,
                    "patient-already-exists", "CPF já cadastrado");
        }

        @Test
        @DisplayName("PatientAlreadyRegisteredException → 409 patient-already-registered")
        void patientAlreadyRegistered() {
            var ex = new PatientAlreadyRegisteredException("Paciente já cadastrado na fila");
            ResponseEntity<ProblemDetail> response = handler.handlePatientAlreadyRegistered(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertProblemDetail(response.getBody(), HttpStatus.CONFLICT,
                    "patient-already-registered", "Paciente já cadastrado na fila");
        }

        @Test
        @DisplayName("ActiveQueueEntriesExistException → 409 active-queue-entries-exist")
        void activeQueueEntriesExist() {
            var ex = new ActiveQueueEntriesExistException("Paciente possui entradas ativas na fila");
            ResponseEntity<ProblemDetail> response = handler.handleActiveQueueEntriesExist(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertProblemDetail(response.getBody(), HttpStatus.CONFLICT,
                    "active-queue-entries-exist", "Paciente possui entradas ativas na fila");
        }

        @Test
        @DisplayName("ProcedureAlreadyAssignedException → 409 procedure-already-assigned")
        void procedureAlreadyAssigned() {
            var ex = new ProcedureAlreadyAssignedException("Procedimento já atribuído ao paciente");
            ResponseEntity<ProblemDetail> response = handler.handleProcedureAlreadyAssigned(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertProblemDetail(response.getBody(), HttpStatus.CONFLICT,
                    "procedure-already-assigned", "Procedimento já atribuído ao paciente");
        }
    }

    // ── 422 Unprocessable Entity ──────────────────────────────────────────────────

    @Nested
    @DisplayName("422 Unprocessable Entity")
    class UnprocessableEntity {

        @Test
        @DisplayName("QueueNotAllowedException → 422 queue-not-allowed")
        void queueNotAllowed() {
            var ex = new QueueNotAllowedException("Status inválido para esta operação");
            ResponseEntity<ProblemDetail> response = handler.handleQueueNotAllowed(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertProblemDetail(response.getBody(), HttpStatus.UNPROCESSABLE_ENTITY,
                    "queue-not-allowed", "Status inválido para esta operação");
        }

        @Test
        @DisplayName("PatientAgeNotEligibleException → 422 age-not-eligible")
        void patientAgeNotEligible() {
            var ex = new PatientAgeNotEligibleException("Paciente fora da faixa etária do procedimento");
            ResponseEntity<ProblemDetail> response = handler.handlePatientAgeNotEligible(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertProblemDetail(response.getBody(), HttpStatus.UNPROCESSABLE_ENTITY,
                    "age-not-eligible", "Paciente fora da faixa etária do procedimento");
        }

        @Test
        @DisplayName("PatientNotEligibleForProcedureException → 422 patient-not-eligible")
        void patientNotEligibleForProcedure() {
            var ex = new PatientNotEligibleForProcedureException("Paciente não elegível para o procedimento");
            ResponseEntity<ProblemDetail> response = handler.handlePatientNotEligibleForProcedure(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertProblemDetail(response.getBody(), HttpStatus.UNPROCESSABLE_ENTITY,
                    "patient-not-eligible", "Paciente não elegível para o procedimento");
        }

        @Test
        @DisplayName("PatientAlreadyInactiveException → 422 patient-already-inactive")
        void patientAlreadyInactive() {
            var ex = new PatientAlreadyInactiveException("Paciente já está inativo");
            ResponseEntity<ProblemDetail> response = handler.handlePatientAlreadyInactive(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertProblemDetail(response.getBody(), HttpStatus.UNPROCESSABLE_ENTITY,
                    "patient-already-inactive", "Paciente já está inativo");
        }

        @Test
        @DisplayName("PatientInactiveException → 422 patient-inactive")
        void patientInactive() {
            var ex = new PatientInactiveException("Paciente está inativo");
            ResponseEntity<ProblemDetail> response = handler.handlePatientInactive(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertProblemDetail(response.getBody(), HttpStatus.UNPROCESSABLE_ENTITY,
                    "patient-inactive", "Paciente está inativo");
        }

        @Test
        @DisplayName("ProcedureNotAssignedException → 422 procedure-not-assigned")
        void procedureNotAssigned() {
            var ex = new ProcedureNotAssignedException("Procedimento não atribuído ao paciente");
            ResponseEntity<ProblemDetail> response = handler.handleProcedureNotAssigned(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertProblemDetail(response.getBody(), HttpStatus.UNPROCESSABLE_ENTITY,
                    "procedure-not-assigned", "Procedimento não atribuído ao paciente");
        }
    }

    // ── 400 Bad Request ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("400 Bad Request")
    class BadRequest {

        @Test
        @DisplayName("MethodArgumentNotValidException → 400 com violations")
        @SuppressWarnings("unchecked")
        void methodArgumentNotValid() {
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("dto", "procedureId", "É obrigatório informar o ID do procedimento!");
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            WebRequest webRequest = mock(WebRequest.class);
            ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                    ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            ProblemDetail body = (ProblemDetail) response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getType().toString()).isEqualTo(TYPE_BASE + "validation-error");
            assertThat(body.getTitle()).isEqualTo("Requisição inválida");
            assertThat(body.getDetail()).isEqualTo("Um ou mais campos são inválidos");

            List<Map<String, String>> violations = (List<Map<String, String>>) body.getProperties().get("violations");
            assertThat(violations).hasSize(1);
            assertThat(violations.get(0)).containsEntry("field", "procedureId");
            assertThat(violations.get(0)).containsEntry("message", "É obrigatório informar o ID do procedimento!");
        }

        @Test
        @DisplayName("violations contém todos os campos inválidos")
        @SuppressWarnings("unchecked")
        void methodArgumentNotValidMultipleFields() {
            BindingResult bindingResult = mock(BindingResult.class);
            List<FieldError> errors = List.of(
                    new FieldError("dto", "procedureId", "campo obrigatório"),
                    new FieldError("dto", "riskColor", "valor inválido")
            );
            when(bindingResult.getFieldErrors()).thenReturn(errors);

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                    ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, mock(WebRequest.class));

            ProblemDetail body = (ProblemDetail) response.getBody();
            List<Map<String, String>> violations = (List<Map<String, String>>) body.getProperties().get("violations");
            assertThat(violations).hasSize(2);
        }
    }

    // ── 500 Internal Server Error ─────────────────────────────────────────────────

    @Nested
    @DisplayName("500 Internal Server Error")
    class InternalServerError {

        @Test
        @DisplayName("Exception genérica → 500 internal-error")
        void handleGeneric() {
            var ex = new RuntimeException("Erro inesperado");
            ResponseEntity<ProblemDetail> response = handler.handleGeneric(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            ProblemDetail body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getType().toString()).isEqualTo(TYPE_BASE + "internal-error");
            assertThat(body.getTitle()).isEqualTo("Erro interno");
            assertThat(body.getDetail()).isEqualTo("Ocorreu um erro inesperado. Tente novamente.");
            assertThat(body.getInstance().toString()).isEqualTo(INSTANCE_URI);
        }
    }

    // ── campos RFC 7807 obrigatórios ──────────────────────────────────────────────

    @Nested
    @DisplayName("Campos RFC 7807")
    class Rfc7807Fields {

        @Test
        @DisplayName("type é URI válida e única por tipo de problema")
        void typeIsUniqueUri() {
            String uri404 = handler.handlePatientNotFound(
                    new PatientNotFoundException("x"), request).getBody().getType().toString();
            String uri409 = handler.handlePatientAlreadyExists(
                    new PatientAlreadyExistsException("x"), request).getBody().getType().toString();
            String uri422 = handler.handleQueueNotAllowed(
                    new QueueNotAllowedException("x"), request).getBody().getType().toString();
            String uri500 = handler.handleGeneric(
                    new RuntimeException("x"), request).getBody().getType().toString();

            assertThat(uri404).startsWith(TYPE_BASE);
            assertThat(uri409).startsWith(TYPE_BASE);
            assertThat(uri422).startsWith(TYPE_BASE);
            assertThat(uri500).startsWith(TYPE_BASE);

            // cada tipo tem slug único
            assertThat(List.of(uri404, uri409, uri422, uri500)).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("instance reflete a URI da requisição")
        void instanceReflectsRequestUri() {
            when(request.getRequestURI()).thenReturn("/api/v1/queue/call-next");
            ResponseEntity<ProblemDetail> response = handler.handleQueueEmpty(
                    new QueueEmptyException("vazia"), request);

            assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/v1/queue/call-next");
        }

        @Test
        @DisplayName("status numérico é consistente com HTTP status da resposta")
        void statusIsConsistent() {
            ResponseEntity<ProblemDetail> r404 = handler.handlePatientNotFound(
                    new PatientNotFoundException("x"), request);
            ResponseEntity<ProblemDetail> r409 = handler.handlePatientAlreadyExists(
                    new PatientAlreadyExistsException("x"), request);
            ResponseEntity<ProblemDetail> r422 = handler.handleQueueNotAllowed(
                    new QueueNotAllowedException("x"), request);

            assertThat(r404.getBody().getStatus()).isEqualTo(r404.getStatusCode().value());
            assertThat(r409.getBody().getStatus()).isEqualTo(r409.getStatusCode().value());
            assertThat(r422.getBody().getStatus()).isEqualTo(r422.getStatusCode().value());
        }
    }
}
