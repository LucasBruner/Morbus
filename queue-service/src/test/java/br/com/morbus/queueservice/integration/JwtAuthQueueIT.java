package br.com.morbus.queueservice.integration;

import br.com.morbus.queueservice.infrastructure.database.repository.PatientJpaRepository;
import br.com.morbus.queueservice.infrastructure.database.repository.ProcedureJpaRepository;
import br.com.morbus.queueservice.infrastructure.database.repository.QueueEntryJpaRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração de autenticação JWT com PostgreSQL e RabbitMQ reais via Testcontainers.
 *
 * <p>Critérios cobertos:</p>
 * <ul>
 *   <li>Token JWT válido com ROLE_MEDICO é aceito pelo queue-service</li>
 *   <li>Token JWT expirado é rejeitado com 401</li>
 *   <li>Token JWT com secret errado é rejeitado com 401</li>
 *   <li>Requisição sem token JWT retorna 401</li>
 *   <li>ROLE_PACIENTE pode consultar posição, mas não cadastrar paciente</li>
 * </ul>
 */
@DisplayName("IT — Autenticação JWT no queue-service")
class JwtAuthQueueIT extends AbstractContainerIT {

    @Autowired PatientJpaRepository patientRepo;
    @Autowired QueueEntryJpaRepository queueEntryRepo;
    @Autowired ProcedureJpaRepository procedureRepo;

    @BeforeEach
    void clean() {
        queueEntryRepo.deleteAll();
        patientRepo.deleteAll();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    // buildJwt(...) e bearerHeaders(...) são herdados de AbstractContainerIT.

    private String buildJwtWithWrongSecret(String username, String role) {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "wrong-secret-key-that-is-different-from-app-secret!".getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(wrongKey)
                .compact();
    }

    private Map<String, Object> patientBody() {
        return Map.of(
                "cpf", "111.444.777-35",
                "nome", "Teste",
                "sobrenome", "JWT",
                "dataNascimento", "1990-01-01",
                "gender", "MASCULINO",
                "grupoLegal", "GERAL"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("JWT MEDICO — aceito")
    class JwtMedicoAceito {

        @Test
        @DisplayName("Token ROLE_MEDICO válido deve ser aceito pelo endpoint de cadastro de paciente")
        void jwt_medico_valido_retorna201() {
            String token = buildJwt("dr.medico", "ROLE_MEDICO", 3_600_000L);
            ResponseEntity<Map> resp = rest.exchange(
                    "http://localhost:" + port + "/api/v1/patients",
                    HttpMethod.POST,
                    new HttpEntity<>(patientBody(), bearerHeaders(token)),
                    Map.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }
    }

    @Nested
    @DisplayName("JWT inválido ou ausente — rejeitado")
    class JwtInvalido {

        @Test
        @DisplayName("Requisição sem token retorna 401")
        void semToken_retorna401() {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = rest.exchange(
                    "http://localhost:" + port + "/api/v1/patients",
                    HttpMethod.POST,
                    new HttpEntity<>(patientBody(), h),
                    String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Token expirado retorna 401")
        void tokenExpirado_retorna401() {
            // Expira em -1ms (já expirado)
            String expiredToken = buildJwt("dr.expirado", "ROLE_MEDICO", -1L);
            ResponseEntity<String> resp = rest.exchange(
                    "http://localhost:" + port + "/api/v1/patients",
                    HttpMethod.POST,
                    new HttpEntity<>(patientBody(), bearerHeaders(expiredToken)),
                    String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Token com secret incorreto retorna 401")
        void tokenSecretErrado_retorna401() {
            String badToken = buildJwtWithWrongSecret("hacker", "ROLE_MEDICO");
            ResponseEntity<String> resp = rest.exchange(
                    "http://localhost:" + port + "/api/v1/patients",
                    HttpMethod.POST,
                    new HttpEntity<>(patientBody(), bearerHeaders(badToken)),
                    String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Token malformado retorna 401")
        void tokenMalformado_retorna401() {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            h.set(HttpHeaders.AUTHORIZATION, "Bearer token.lixo.qualquer");
            ResponseEntity<String> resp = rest.exchange(
                    "http://localhost:" + port + "/api/v1/patients",
                    HttpMethod.POST,
                    new HttpEntity<>(patientBody(), h),
                    String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("RBAC — controle de acesso por role")
    class Rbac {

        @Test
        @DisplayName("ROLE_PACIENTE não pode cadastrar paciente (403)")
        void paciente_naoPodeCadastrarPaciente_retorna403() {
            String token = buildJwt("usuario.paciente", "ROLE_PACIENTE", 3_600_000L);
            ResponseEntity<String> resp = rest.exchange(
                    "http://localhost:" + port + "/api/v1/patients",
                    HttpMethod.POST,
                    new HttpEntity<>(patientBody(), bearerHeaders(token)),
                    String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("ROLE_PACIENTE pode consultar posição na fila (200)")
        void paciente_podeConsultarPosicao_retorna200ouNaoEncontrado() {
            // Primeiro cadastrar o paciente e enfileirá-lo via MEDICO
            String medicoToken = buildJwt("dr.medico2", "ROLE_MEDICO", 3_600_000L);
            ResponseEntity<Map> patResp = rest.exchange(
                    "http://localhost:" + port + "/api/v1/patients",
                    HttpMethod.POST,
                    new HttpEntity<>(patientBody(), bearerHeaders(medicoToken)),
                    Map.class);
            UUID patientId = UUID.fromString((String) patResp.getBody().get("id"));

            UUID procedureId = procedureRepo.findAll().stream()
                    .filter(p -> p.getIdadeMinima() == 0)
                    .findFirst()
                    .get().getId();

            Map<String, Object> enqBody = Map.of(
                    "patientId", patientId.toString(),
                    "procedureId", procedureId.toString(),
                    "riskColor", "AZUL",
                    "tipoFila", "FILA_REGULADA"
            );
            ResponseEntity<Map> enqResp = rest.exchange(
                    "http://localhost:" + port + "/api/v1/queue",
                    HttpMethod.POST,
                    new HttpEntity<>(enqBody, bearerHeaders(medicoToken)),
                    Map.class);
            String entryId = (String) enqResp.getBody().get("id");

            // Agora consulta com ROLE_PACIENTE
            String pacienteToken = buildJwt("usuario.paciente", "ROLE_PACIENTE", 3_600_000L);
            ResponseEntity<Map> posResp = rest.exchange(
                    "http://localhost:" + port + "/api/v1/queue/" + entryId + "/position",
                    HttpMethod.GET,
                    new HttpEntity<>(bearerHeaders(pacienteToken)),
                    Map.class);
            assertThat(posResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
