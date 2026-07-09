package br.com.morbus.queueservice.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Base para todos os testes de integração com Testcontainers.
 *
 * <p>Inicia um PostgreSQL e um RabbitMQ reais (via Docker) compartilhados por toda
 * a suite de testes de integração. Os containers são criados uma única vez e
 * reutilizados entre as subclasses para reduzir o tempo de startup.</p>
 *
 * <p>Perfil {@code integration} habilita Flyway real e desabilita as sobreposições
 * de H2 definidas em {@code application.properties} de teste.</p>
 *
 * <p>Também centraliza os helpers de autenticação JWT e o {@link RestTemplate} com
 * error handler permissivo (necessário para capturar respostas 4xx/5xx como
 * {@code ResponseEntity} em vez de lançar exceção), compartilhados por todas as
 * subclasses que fazem chamadas HTTP autenticadas.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Testcontainers
public abstract class AbstractContainerIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sus_queue_db")
                    .withUsername("sus_user")
                    .withPassword("sus_pass");

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:3-management-alpine")
                    .withUser("admin", "admin");

    @LocalServerPort
    protected int port;

    @Value("${jwt.secret}")
    protected String jwtSecret;

    // RestTemplate com error handler permissivo para capturar 4xx/5xx como ResponseEntity
    protected final RestTemplate rest = new RestTemplate() {{
        setErrorHandler(new DefaultResponseErrorHandler() {
            @Override public boolean hasError(ClientHttpResponse r) { return false; }
        });
    }};

    protected String buildJwt(String username, String role, long expiresInMs) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiresInMs))
                .signWith(key)
                .compact();
    }

    protected String buildJwt(String username, String role) {
        return buildJwt(username, role, 3_600_000L);
    }

    protected HttpHeaders bearerHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }
}
