package br.com.morbus.queueservice.integration;

import br.com.morbus.queueservice.domain.enums.EDestino;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.infrastructure.database.entity.ProcedureEntity;
import br.com.morbus.queueservice.infrastructure.database.repository.PatientJpaRepository;
import br.com.morbus.queueservice.infrastructure.database.repository.ProcedureJpaRepository;
import br.com.morbus.queueservice.infrastructure.database.repository.QueueEntryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração do fluxo do paciente com PostgreSQL e RabbitMQ reais via Testcontainers.
 *
 * <p>Critérios cobertos:</p>
 * <ul>
 *   <li>Cadastrar paciente → inserir em FILA_REGULADA → consultar posição → chamar próximo → validar AGENDADO</li>
 *   <li>Inserir em FILA_ESPERA sempre recebe cor AZUL (forçada pelo servidor)</li>
 *   <li>Reclassificar cor VERDE → VERMELHO e validar reordenação da fila</li>
 *   <li>Tentativa de reclassificação em FILA_ESPERA retorna 422</li>
 *   <li>Tentativa de reclassificação de entrada com status não-permitido retorna 422</li>
 * </ul>
 */
@DisplayName("IT — Fluxo do paciente na fila")
class QueuePatientFlowIT extends AbstractContainerIT {

    @Autowired PatientJpaRepository patientRepo;
    @Autowired ProcedureJpaRepository procedureRepo;
    @Autowired QueueEntryJpaRepository queueEntryRepo;

    private UUID procedureId;

    // Intervalo mínimo entre inserções para garantir registeredAt distintos (mesma
    // convenção usada em QueueFlowIntegrationTest.OrdenacaoPorRegisteredAt).
    private static final long ARRIVAL_ORDER_GAP_MS = 50;

    @BeforeEach
    void cleanAndSetup() {
        queueEntryRepo.deleteAll();
        patientRepo.deleteAll();
        procedureId = procedureRepo.findAll().stream()
                .filter(p -> p.getIdadeMinima() == 0 && p.getIdadeMaxima() >= 60)
                .findFirst()
                .map(ProcedureEntity::getId)
                .orElseThrow(() -> new IllegalStateException("Nenhum procedimento geral disponível no seed"));
    }

    // ─────────────────── helpers ───────────────────────────────────────────────
    // buildJwt(...) e bearerHeaders(...) são herdados de AbstractContainerIT.

    private String jwtMedico() {
        return buildJwt("dr.integração", "ROLE_MEDICO");
    }

    private String jwtPaciente() {
        return buildJwt("paciente.it", "ROLE_PACIENTE");
    }

    private UUID registerPatient(String cpf, String nome, LocalDate dob, EPriorityGroup grupo) {
        Map<String, Object> body = Map.of(
                "cpf", cpf,
                "nome", nome,
                "sobrenome", "Sobrenome",
                "dataNascimento", dob.toString(),
                "gender", "MASCULINO",
                "grupoLegal", grupo.name()
        );
        ResponseEntity<Map> resp = rest.exchange(
                "http://localhost:" + port + "/api/v1/patients",
                HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(jwtMedico())),
                Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) resp.getBody().get("id"));
    }

    // A cor de entrada é sempre AZUL (forçada pelo servidor); use reclassify() para alterar depois.
    private ResponseEntity<Map> enqueue(UUID patientId, EDestino tipoFila) {
        Map<String, Object> body = Map.of(
                "patientId", patientId.toString(),
                "procedureId", procedureId.toString(),
                "tipoFila", tipoFila.name()
        );
        return rest.exchange(
                "http://localhost:" + port + "/api/v1/queue",
                HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(jwtMedico())),
                Map.class);
    }

    private ResponseEntity<Map> reclassify(String entryId, ERiskColor color) {
        Map<String, String> body = Map.of("riskColor", color.name());
        return rest.exchange(
                "http://localhost:" + port + "/api/v1/queue/" + entryId + "/priority",
                HttpMethod.PATCH,
                new HttpEntity<>(body, bearerHeaders(jwtMedico())),
                Map.class);
    }

    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Fluxo completo FILA_REGULADA")
    class FluxoFilaReguladaCompleto {

        @Test
        @DisplayName("Cadastrar paciente → FILA_REGULADA → posição → callNext → CHAMADO")
        void fluxoCompleto_filaRegulada() {
            UUID patientId = registerPatient("111.444.777-35", "João", LocalDate.of(1990, 1, 1), EPriorityGroup.GERAL);

            // Inserir na FILA_REGULADA (cor sempre AZUL na criação)
            ResponseEntity<Map> enqResp = enqueue(patientId, EDestino.FILA_REGULADA);
            assertThat(enqResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(enqResp.getBody().get("riskColor")).isEqualTo("AZUL");
            String entryId = (String) enqResp.getBody().get("id");
            assertThat(enqResp.getBody().get("status")).isEqualTo("AGUARDANDO");

            // Consultar posição
            ResponseEntity<Map> posResp = rest.exchange(
                    "http://localhost:" + port + "/api/v1/queue/" + entryId + "/position",
                    HttpMethod.GET,
                    new HttpEntity<>(bearerHeaders(jwtMedico())),
                    Map.class);
            assertThat(posResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat((Integer) posResp.getBody().get("position")).isGreaterThanOrEqualTo(1);

            // Chamar próximo
            ResponseEntity<Map> callResp = rest.exchange(
                    "http://localhost:" + port + "/api/v1/queue/call-next",
                    HttpMethod.POST,
                    new HttpEntity<>(bearerHeaders(jwtMedico())),
                    Map.class);
            assertThat(callResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(callResp.getBody().get("status")).isEqualTo("CHAMADO");
            assertThat(callResp.getBody().get("id")).isEqualTo(entryId);
        }
    }

    @Nested
    @DisplayName("FILA_ESPERA — validações")
    class FilaEsperaValidacoes {

        @Test
        @DisplayName("Inserir em FILA_ESPERA é sempre aceito com cor AZUL")
        void filaEspera_sempreAzul_retorna201() {
            UUID patientId = registerPatient("333.666.999-35", "Carlos", LocalDate.of(1988, 7, 20), EPriorityGroup.GERAL);

            ResponseEntity<Map> resp = enqueue(patientId, EDestino.FILA_ESPERA);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
            assertThat(resp.getBody().get("riskColor")).isEqualTo("AZUL");
        }

        @Test
        @DisplayName("Tentativa de reclassificação em FILA_ESPERA retorna 422")
        void reclassificarFilaEspera_retorna422() {
            UUID patientId = registerPatient("444.777.111-35", "Ana", LocalDate.of(1992, 11, 5), EPriorityGroup.GERAL);
            ResponseEntity<Map> enqResp = enqueue(patientId, EDestino.FILA_ESPERA);
            String entryId = (String) enqResp.getBody().get("id");

            // Tentar reclassificar → deve falhar com 422
            ResponseEntity<Map> resp = reclassify(entryId, ERiskColor.VERMELHO);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(422));
        }
    }

    @Nested
    @DisplayName("Reclassificação de prioridade — FILA_REGULADA")
    class ReclassificacaoFilaRegulada {

        @Test
        @DisplayName("Reclassificar VERDE → VERMELHO e verificar que o paciente sobe na fila")
        void reclassificar_verde_para_vermelho_subeNaFila() throws InterruptedException {
            UUID pacienteVerde = registerPatient("555.888.222-35", "Pedro", LocalDate.of(1983, 2, 14), EPriorityGroup.GERAL);
            UUID pacienteAmarelo = registerPatient("666.999.333-35", "Rosa", LocalDate.of(1987, 6, 18), EPriorityGroup.GERAL);

            // Ambos entram como AZUL; reclassificamos para os valores desejados do cenário.
            ResponseEntity<Map> enqVerde = enqueue(pacienteVerde, EDestino.FILA_REGULADA);
            String entryVerde = (String) enqVerde.getBody().get("id");
            reclassify(entryVerde, ERiskColor.VERDE);
            Thread.sleep(ARRIVAL_ORDER_GAP_MS);

            ResponseEntity<Map> enqAmarelo = enqueue(pacienteAmarelo, EDestino.FILA_REGULADA);
            reclassify((String) enqAmarelo.getBody().get("id"), ERiskColor.AMARELO);

            // Posição de Pedro (VERDE) deve ser 2 (atrás de Rosa)
            ResponseEntity<Map> pos1 = rest.exchange(
                    "http://localhost:" + port + "/api/v1/queue/" + entryVerde + "/position",
                    HttpMethod.GET,
                    new HttpEntity<>(bearerHeaders(jwtMedico())),
                    Map.class);
            assertThat((Integer) pos1.getBody().get("position")).isEqualTo(2);

            // Reclassificar Pedro para VERMELHO
            ResponseEntity<Map> reclassResp = reclassify(entryVerde, ERiskColor.VERMELHO);
            assertThat(reclassResp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(reclassResp.getBody().get("riskColor")).isEqualTo("VERMELHO");

            // Posição de Pedro agora deve ser 1 (VERMELHO precede AMARELO)
            ResponseEntity<Map> pos2 = rest.exchange(
                    "http://localhost:" + port + "/api/v1/queue/" + entryVerde + "/position",
                    HttpMethod.GET,
                    new HttpEntity<>(bearerHeaders(jwtMedico())),
                    Map.class);
            assertThat((Integer) pos2.getBody().get("position")).isEqualTo(1);
        }

        @Test
        @DisplayName("Reclassificação de entrada CHAMADO retorna 422")
        void reclassificar_entradaChamado_retorna422() {
            UUID patientId = registerPatient("777.111.444-35", "Luís", LocalDate.of(1975, 9, 22), EPriorityGroup.GERAL);
            ResponseEntity<Map> enqResp = enqueue(patientId, EDestino.FILA_REGULADA);
            String entryId = (String) enqResp.getBody().get("id");

            // Chamar o próximo (transiciona para CHAMADO)
            rest.exchange("http://localhost:" + port + "/api/v1/queue/call-next",
                    HttpMethod.POST, new HttpEntity<>(bearerHeaders(jwtMedico())), Map.class);

            // Tentar reclassificar entrada AGENDADO → 422
            ResponseEntity<Map> resp = reclassify(entryId, ERiskColor.VERMELHO);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(422));
        }
    }
}
