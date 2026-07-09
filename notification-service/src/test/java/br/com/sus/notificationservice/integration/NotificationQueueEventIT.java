package br.com.sus.notificationservice.integration;

import br.com.sus.notificationservice.model.Notification;
import br.com.sus.notificationservice.model.enums.ENotificationType;
import br.com.sus.notificationservice.service.EmailService;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.RabbitMQContainer;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.Mockito.verify;

/**
 * Testes de integração do notification-service com PostgreSQL e RabbitMQ reais via Testcontainers.
 *
 * <p>Critérios cobertos (fluxo de notificação):</p>
 * <ul>
 *   <li>{@code patient.registered} publicado gera notificação de registro (mailer mockado)</li>
 *   <li>{@code patient.called} publicado gera notificação de chamada (mailer mockado)</li>
 *   <li>{@code EmailService} não envia e-mails reais — mock verificado</li>
 * </ul>
 */
@QuarkusTest
@QuarkusTestResource(NotificationContainerResource.class)
@DisplayName("IT — Eventos RabbitMQ → notificações no notification-service")
class NotificationQueueEventIT {

    private static final String EXCHANGE  = "sus.queue.exchange";
    private static final String RK_REGISTERED = "patient.registered";
    private static final String RK_CALLED     = "patient.called";

    /** Injetado pelo {@link
     * NotificationContainerResource
     * #inject(TestInjector)}. **/
    @InjectRabbit
    RabbitMQContainer rabbit;

    /** Mailer mockado — nenhum e-mail real é enviado durante os testes. */
    @InjectMock
    EmailService emailService;

    // ─────────────────────────────────────────────────────────────────────────

    @BeforeEach
    void cleanNotifications() {
        // Limpa notificações antes de cada teste para isolamento
        Notification.deleteAll();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Publica um payload serializado como JSON diretamente no exchange RabbitMQ do container de teste.
     *
     * @param routingKey routing key (ex: "patient.registered")
     * @param payload    objeto serializável para JSON
     */
    private void publishToRabbit(String routingKey, Object payload) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbit.getHost());
        factory.setPort(rabbit.getAmqpPort());
        factory.setUsername("admin");
        factory.setPassword("admin");

        try (Connection conn = factory.newConnection();
             Channel ch = conn.createChannel()) {
            ch.exchangeDeclarePassive(EXCHANGE);

            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            byte[] body = mapper.writeValueAsBytes(payload);

            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .build();

            ch.basicPublish(EXCHANGE, routingKey, props, body);
        }
    }

    private Map<String, Object> buildQueueEvent(String eventType) {
        return Map.of(
                "eventType",     eventType,
                "queueEntryId",  UUID.randomUUID().toString(),
                "patientName",   "João da Silva",
                "patientContact","joao@email.com",
                "procedureName", "Consulta Cardiológica",
                "riskColor",     "AMARELO",
                "timestamp",     LocalDateTime.now().toString()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("patient.registered → notificação de registro")
    class PatientRegistered {

        @Test
        @DisplayName("Evento patient.registered gera notificação com status ENVIADO")
        void patientRegistered_geraNotificacao() throws Exception {
            publishToRabbit(RK_REGISTERED, buildQueueEvent("PATIENT_REGISTERED"));

            // Aguarda processamento assíncrono (máximo 10s)
            Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .pollInterval(200, TimeUnit.MILLISECONDS)
                    .untilAsserted(() ->
                            given()
                                    .queryParam("eventType", "PATIENT_REGISTERED")
                                    .when().get("/api/v1/notifications")
                                    .then().statusCode(200)
                                    .body("$", hasSize(greaterThanOrEqualTo(1)))
                    );

            // Verifica que a notificação foi persistida com os dados corretos
            Notification saved = Notification
                    .find("eventType", ENotificationType.PATIENT_REGISTERED.name())
                    .firstResult();
            assertThat(saved).isNotNull();
            assertThat(saved.recipientName).isEqualTo("João da Silva");
            assertThat(saved.status).isEqualTo("ENVIADO");
            assertThat(saved.message).contains("Consulta Cardiológica");
        }

        @Test
        @DisplayName("Mailer é acionado mas não envia e-mail real (mock verificado)")
        void patientRegistered_mailerMockado_naoEnviaEmailReal() throws Exception {
            publishToRabbit(RK_REGISTERED, buildQueueEvent("PATIENT_REGISTERED"));

            Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .pollInterval(200, TimeUnit.MILLISECONDS)
                    .until(() -> Notification.count("eventType", ENotificationType.PATIENT_REGISTERED.name()) > 0);

            // Verifica que o mock foi chamado (não email real, apenas chamada simulada)
            verify(emailService).send(
                    org.mockito.ArgumentMatchers.eq("joao@email.com"),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString()
            );
        }
    }

    @Nested
    @DisplayName("patient.called → notificação de chamada")
    class PatientCalled {

        @Test
        @DisplayName("Evento patient.called gera notificação de chamada com status ENVIADO")
        void patientCalled_geraNotificacao() throws Exception {
            publishToRabbit(RK_CALLED, buildQueueEvent("PATIENT_CALLED"));

            Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .pollInterval(200, TimeUnit.MILLISECONDS)
                    .untilAsserted(() ->
                            given()
                                    .queryParam("eventType", "PATIENT_CALLED")
                                    .when().get("/api/v1/notifications")
                                    .then().statusCode(200)
                                    .body("$", hasSize(greaterThanOrEqualTo(1)))
                    );

            Notification saved = Notification
                    .find("eventType", ENotificationType.PATIENT_CALLED.name())
                    .firstResult();
            assertThat(saved).isNotNull();
            assertThat(saved.status).isEqualTo("ENVIADO");
        }

        @Test
        @DisplayName("Múltiplos eventos processados independentemente — cada um gera sua notificação")
        void multiplosEventos_geramNotificacoesIndependentes() throws Exception {
            publishToRabbit(RK_REGISTERED, buildQueueEvent("PATIENT_REGISTERED"));
            publishToRabbit(RK_CALLED,     buildQueueEvent("PATIENT_CALLED"));

            Awaitility.await()
                    .atMost(15, TimeUnit.SECONDS)
                    .pollInterval(200, TimeUnit.MILLISECONDS)
                    .until(() -> Notification.count() >= 2);

            long registeredCount = Notification.count("eventType", ENotificationType.PATIENT_REGISTERED.name());
            long calledCount     = Notification.count("eventType", ENotificationType.PATIENT_CALLED.name());

            assertThat(registeredCount).isGreaterThanOrEqualTo(1);
            assertThat(calledCount).isGreaterThanOrEqualTo(1);
        }
    }
}
