package br.com.sus.notificationservice.controller;

import br.com.sus.notificationservice.model.Notification;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.panache.common.Sort;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
@DisplayName("NotificationController")
class NotificationControllerTest {

    private static final String PATH = "/api/v1/notifications";

    @BeforeEach
    void setUp() {
        PanacheMock.mock(Notification.class);
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Notification buildNotification(Long id, String eventType, String recipientName) {
        Notification n = new Notification();
        n.id = id;
        n.eventType = eventType;
        n.recipientName = recipientName;
        n.recipientContact = "contato@email.com";
        n.message = "Mensagem de teste";
        n.sentAt = LocalDateTime.of(2025, 6, 1, 10, 0);
        n.status = "ENVIADO";
        return n;
    }

    // ── GET /api/v1/notifications ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/notifications")
    class GetAll {

        @Test
        @DisplayName("deve retornar 200 com lista de notificações")
        void deveRetornar200ComListaDeNotificacoes() {
            Notification n1 = buildNotification(1L, "PATIENT_CALLED", "João Silva");
            Notification n2 = buildNotification(2L, "PATIENT_REGISTERED", "Maria Souza");
            when(Notification.listAll(any(Sort.class))).thenReturn(List.of(n1, n2));

            given()
                    .when().get(PATH)
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(2))
                    .body("[0].recipientName", equalTo("João Silva"))
                    .body("[0].eventType", equalTo("PATIENT_CALLED"))
                    .body("[1].recipientName", equalTo("Maria Souza"));
        }

        @Test
        @DisplayName("deve retornar 200 com lista vazia quando não há notificações")
        void deveRetornar200ComListaVazia() {
            when(Notification.listAll(any(Sort.class))).thenReturn(List.of());

            given()
                    .when().get(PATH)
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(0));
        }

        @Test
        @DisplayName("deve retornar notificações filtradas por eventType")
        void deveRetornarNotificacoesFiltradas() {
            Notification n = buildNotification(1L, "PATIENT_CALLED", "Carlos Lima");
            when(Notification.list(anyString(), any(Sort.class), any(Object[].class)))
                    .thenReturn(List.of(n));

            given()
                    .queryParam("eventType", "PATIENT_CALLED")
                    .when().get(PATH)
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(1))
                    .body("[0].eventType", equalTo("PATIENT_CALLED"));
        }

        @Test
        @DisplayName("deve retornar lista vazia quando filtro não encontra resultados")
        void deveRetornarListaVaziaQuandoFiltroSemResultados() {
            // PanacheMock retorna lista vazia por padrão para chamadas não-stubadas
            // Não configuramos stub para evitar conflito de arity causado por estado do Mockito entre testes

            given()
                    .queryParam("eventType", "PATIENT_CANCELLED")
                    .when().get(PATH)
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(0));
        }
    }

    // ── GET /api/v1/notifications/{id} ────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/notifications/{id}")
    class GetById {

        @Test
        @DisplayName("deve retornar 200 com a notificação quando encontrada")
        void deveRetornar200QuandoNotificacaoEncontrada() {
            Notification n = buildNotification(1L, "PATIENT_REGISTERED", "Ana Costa");
            when(Notification.findById(1L)).thenReturn(n);

            given()
                    .when().get(PATH + "/1")
                    .then()
                    .statusCode(200)
                    .body("id", equalTo(1))
                    .body("recipientName", equalTo("Ana Costa"))
                    .body("eventType", equalTo("PATIENT_REGISTERED"))
                    .body("status", equalTo("ENVIADO"));
        }

        @Test
        @DisplayName("deve retornar 404 quando notificação não é encontrada")
        void deveRetornar404QuandoNotificacaoNaoEncontrada() {
            when(Notification.findById(any())).thenReturn(null);

            given()
                    .when().get(PATH + "/99999")
                    .then()
                    .statusCode(404);
        }
    }
}
