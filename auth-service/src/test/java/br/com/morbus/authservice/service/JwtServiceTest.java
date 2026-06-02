package br.com.morbus.authservice.service;

import br.com.morbus.authservice.model.User;
import br.com.morbus.authservice.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    // Secret com 256+ bits para HMAC-SHA256
    private static final String SECRET      = "test-secret-key-with-at-least-256-bits-for-hmac-sha256-ok";
    private static final long   EXPIRATION  = 86_400_000L; // 24h

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION);
    }

    private User buildUser(String username, UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    // ── generateToken ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("deve gerar token não nulo e não vazio")
        void generatesNonEmptyToken() {
            User user = buildUser("dr_silva", UserRole.MEDICO);
            String token = jwtService.generateToken(user);
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("token deve ter 3 partes separadas por ponto (header.payload.signature)")
        void tokenHasThreeParts() {
            User user = buildUser("dr_silva", UserRole.MEDICO);
            String token = jwtService.generateToken(user);
            assertThat(token.split("\\.")).hasSize(3);
        }
    }

    // ── validateToken ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("deve retornar true para token válido recém-gerado")
        void validToken() {
            User user = buildUser("dr_silva", UserRole.MEDICO);
            String token = jwtService.generateToken(user);
            assertThat(jwtService.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("deve retornar false para token expirado")
        void expiredToken() {
            JwtService shortLived = new JwtService(SECRET, 1L); // expira em 1ms
            User user = buildUser("dr_silva", UserRole.MEDICO);
            String token = shortLived.generateToken(user);

            // Aguarda expirar
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}

            assertThat(shortLived.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para token com assinatura adulterada")
        void tamperedSignature() {
            User user = buildUser("dr_silva", UserRole.MEDICO);
            String token = jwtService.generateToken(user);
            String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "assinaturafalsa";
            assertThat(jwtService.validateToken(tampered)).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para string aleatória")
        void randomString() {
            assertThat(jwtService.validateToken("nao.e.um.jwt")).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para token vazio")
        void emptyToken() {
            assertThat(jwtService.validateToken("")).isFalse();
        }
    }

    // ── extractUsername ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("deve extrair o username correto do token")
        void extractsCorrectUsername() {
            User user = buildUser("dr_silva", UserRole.MEDICO);
            String token = jwtService.generateToken(user);
            assertThat(jwtService.extractUsername(token)).isEqualTo("dr_silva");
        }

        @Test
        @DisplayName("deve extrair username correto para PACIENTE")
        void extractsCorrectUsernameForPaciente() {
            User user = buildUser("paciente_joao", UserRole.PACIENTE);
            String token = jwtService.generateToken(user);
            assertThat(jwtService.extractUsername(token)).isEqualTo("paciente_joao");
        }
    }

    // ── extractRole ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractRole")
    class ExtractRole {

        @Test
        @DisplayName("deve extrair MEDICO corretamente")
        void extractsMedico() {
            User user = buildUser("dr_silva", UserRole.MEDICO);
            String token = jwtService.generateToken(user);
            assertThat(jwtService.extractRole(token)).isEqualTo("MEDICO");
        }

        @Test
        @DisplayName("deve extrair PACIENTE corretamente")
        void extractsPaciente() {
            User user = buildUser("joao", UserRole.PACIENTE);
            String token = jwtService.generateToken(user);
            assertThat(jwtService.extractRole(token)).isEqualTo("PACIENTE");
        }

        @Test
        @DisplayName("username e role devem ser independentes entre tokens diferentes")
        void tokensAreIndependent() {
            User medico   = buildUser("dr_silva", UserRole.MEDICO);
            User paciente = buildUser("joao",     UserRole.PACIENTE);

            String tokenMedico   = jwtService.generateToken(medico);
            String tokenPaciente = jwtService.generateToken(paciente);

            assertThat(jwtService.extractRole(tokenMedico)).isEqualTo("MEDICO");
            assertThat(jwtService.extractRole(tokenPaciente)).isEqualTo("PACIENTE");
            assertThat(jwtService.extractUsername(tokenMedico)).isEqualTo("dr_silva");
            assertThat(jwtService.extractUsername(tokenPaciente)).isEqualTo("joao");
        }
    }
}
