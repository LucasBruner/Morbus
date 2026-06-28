package br.com.morbus.regulacao.adapters.out.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-chars!!";

    private JwtService jwtService;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET);
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private String buildToken(String subject, String role, String unitId, long expirationMs) {
        var builder = Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey);
        if (unitId != null) {
            builder.claim("unit_id", unitId);
        }
        return builder.compact();
    }

    // ── validateToken ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("deve retornar true para token válido")
        void deveRetornarTrueParaTokenValido() {
            String token = buildToken(UUID.randomUUID().toString(), "MEDICO", null, 86_400_000L);
            assertThat(jwtService.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("deve retornar false para token expirado")
        void deveRetornarFalseParaTokenExpirado() {
            String token = buildToken(UUID.randomUUID().toString(), "MEDICO", null, -1000L);
            assertThat(jwtService.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para token malformado")
        void deveRetornarFalseParaTokenMalformado() {
            assertThat(jwtService.validateToken("nao.e.um.jwt")).isFalse();
        }

        @Test
        @DisplayName("deve retornar false para token em branco")
        void deveRetornarFalseParaTokenEmBranco() {
            assertThat(jwtService.validateToken("")).isFalse();
        }
    }

    // ── extractUsername ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("deve extrair o subject (userId) corretamente")
        void deveExtrairUsername() {
            String userId = UUID.randomUUID().toString();
            String token = buildToken(userId, "SOLICITANTE", null, 86_400_000L);
            assertThat(jwtService.extractUsername(token)).isEqualTo(userId);
        }
    }

    // ── extractRole ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractRole")
    class ExtractRole {

        @Test
        @DisplayName("deve extrair a role corretamente")
        void deveExtrairRole() {
            String token = buildToken(UUID.randomUUID().toString(), "MEDICO", null, 86_400_000L);
            assertThat(jwtService.extractRole(token)).isEqualTo("MEDICO");
        }
    }

    // ── extractUnitId ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractUnitId")
    class ExtractUnitId {

        @Test
        @DisplayName("deve extrair unitId quando presente")
        void deveExtrairUnitId() {
            String unitId = UUID.randomUUID().toString();
            String token = buildToken(UUID.randomUUID().toString(), "SOLICITANTE", unitId, 86_400_000L);
            assertThat(jwtService.extractUnitId(token)).isEqualTo(unitId);
        }

        @Test
        @DisplayName("deve retornar null quando unitId não está no token")
        void deveRetornarNuloQuandoAusente() {
            String token = buildToken(UUID.randomUUID().toString(), "MEDICO", null, 86_400_000L);
            assertThat(jwtService.extractUnitId(token)).isNull();
        }
    }
}
