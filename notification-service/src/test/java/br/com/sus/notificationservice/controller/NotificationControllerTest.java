package br.com.sus.notificationservice.controller;

import br.com.sus.notificationservice.model.Notification;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Sort;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private PanacheQuery mockQuery(List<Notification> content) {
        PanacheQuery query = Mockito.mock(PanacheQuery.class);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(content);
        when(query.count()).thenReturn((long) content.size());
        when(query.pageCount()).thenReturn(content.isEmpty() ? 0 : 1);
        return query;
    }

    // ── GET /api/v1/notifications ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/notifications")
    class GetAll {

        @Test
        @DisplayName("deve retornar 200 com página de notificações")
        void retorna200ComListaDeNotificacoes() {
            Notification n1 = buildNotification(1L, "PATIENT_CALLED", "João Silva");
            Notification n2 = buildNotification(2L, "PATIENT_REGISTERED", "Maria Souza");
            when(Notification.findAll(any(Sort.class))).thenReturn(mockQuery(List.of(n1, n2)));

            given()
                    .when().get(PATH)
                    .then()
                    .statusCode(200)
                    .body("content", hasSize(2))
                    .body("page", equalTo(0))
                    .body("size", equalTo(20))
                    .body("totalElements", equalTo(2))
                    .body("content[0].recipientName", equalTo("João Silva"))
                    .body("content[0].eventType", equalTo("PATIENT_CALLED"))
                    .body("content[1].recipientName", equalTo("Maria Souza"));
        }

        @Test
        @DisplayName("deve retornar 200 com página vazia quando não há notificações")
        void retorna200ComListaVazia() {
            when(Notification.findAll(any(Sort.class))).thenReturn(mockQuery(List.of()));

            given()
                    .when().get(PATH)
                    .then()
                    .statusCode(200)
                    .body("content", hasSize(0));
        }

        @Test
        @DisplayName("deve retornar notificações filtradas por eventType")
        void retornaNotificacoesFiltradas() {
            Notification n = buildNotification(1L, "PATIENT_CALLED", "Carlos Lima");
            when(Notification.find(anyString(), any(Sort.class), any(Object[].class)))
                    .thenReturn(mockQuery(List.of(n)));

            given()
                    .queryParam("eventType", "PATIENT_CALLED")
                    .when().get(PATH)
                    .then()
                    .statusCode(200)
                    .body("content", hasSize(1))
                    .body("content[0].eventType", equalTo("PATIENT_CALLED"));
        }

        @Test
        @DisplayName("deve respeitar page e size informados")
        void respeitaPageESizeInformados() {
            Notification n = buildNotification(1L, "PATIENT_CALLED", "Carlos Lima");
            when(Notification.findAll(any(Sort.class))).thenReturn(mockQuery(List.of(n)));

            given()
                    .queryParam("page", 2)
                    .queryParam("size", 5)
                    .when().get(PATH)
                    .then()
                    .statusCode(200)
                    .body("page", equalTo(2))
                    .body("size", equalTo(5));
        }
    }

    // ── GET /api/v1/notifications/{id} ────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/notifications/{id}")
    class GetById {

        @Test
        @DisplayName("deve retornar 200 com a notificação quando encontrada")
        void retorna200NotificacaoEncontrada() {
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
        void retorna404NotificacaoNaoEncontrada() {
            when(Notification.findById(any())).thenReturn(null);

            given()
                    .when().get(PATH + "/99999")
                    .then()
                    .statusCode(404)
                    .contentType("application/problem+json")
                    .body("type", equalTo("https://morbus.sus.gov.br/problems/notification-not-found"))
                    .body("status", equalTo(404));
        }
    }
}
