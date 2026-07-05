package br.com.sus.notificationservice.service;

import br.com.sus.notificationservice.model.Notification;
import br.com.sus.notificationservice.model.dto.AppointmentConfirmedEventDTO;
import br.com.sus.notificationservice.model.dto.AppointmentNoSlotEventDTO;
import br.com.sus.notificationservice.model.dto.QueueEventDTO;
import br.com.sus.notificationservice.model.dto.SolicitacaoDevolvidaEventDTO;
import br.com.sus.notificationservice.model.dto.SolicitacaoNegadaEventDTO;
import br.com.sus.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService")
class NotificationServiceTest {

    @Mock
    NotificationRepository notificationRepository;

    @Mock
    EmailService emailService;

    @InjectMocks
    NotificationService notificationService;

    private QueueEventDTO buildEvent(String eventType) {
        return new QueueEventDTO(
                eventType,
                UUID.randomUUID(),
                "João Silva",
                "joao@email.com",
                "Consulta Cardiológica",
                "VERMELHO",
                LocalDateTime.of(2025, 6, 1, 10, 0)
        );
    }

    // ── process() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("process()")
    class Process {

        @Test
        @DisplayName("deve persistir notificação e enviar email para PATIENT_REGISTERED")
        void deveProcessarPatientRegistered() {
            QueueEventDTO event = buildEvent("PATIENT_REGISTERED");

            notificationService.process(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).persist(captor.capture());
            Notification saved = captor.getValue();

            assertThat(saved.eventType).isEqualTo("PATIENT_REGISTERED");
            assertThat(saved.recipientName).isEqualTo("João Silva");
            assertThat(saved.recipientContact).isEqualTo("joao@email.com");
            assertThat(saved.status).isEqualTo("ENVIADO");
            assertThat(saved.message)
                    .contains("Consulta Cardiológica")
                    .contains("VERMELHO");

            verify(emailService).send("joao@email.com", "Morbus Notification", saved.message);
        }

        @Test
        @DisplayName("deve persistir notificação e enviar email para PATIENT_CALLED")
        void deveProcessarPatientCalled() {
            QueueEventDTO event = buildEvent("PATIENT_CALLED");

            notificationService.process(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).persist(captor.capture());
            Notification saved = captor.getValue();

            assertThat(saved.eventType).isEqualTo("PATIENT_CALLED");
            assertThat(saved.status).isEqualTo("ENVIADO");
            assertThat(saved.message).contains("Consulta Cardiológica");
        }

        @Test
        @DisplayName("deve persistir notificação e enviar email para PRIORITY_UPDATED")
        void deveProcessarPriorityUpdated() {
            QueueEventDTO event = buildEvent("PRIORITY_UPDATED");

            notificationService.process(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).persist(captor.capture());
            Notification saved = captor.getValue();

            assertThat(saved.status).isEqualTo("ENVIADO");
            assertThat(saved.message).contains("VERMELHO");
        }

        @Test
        @DisplayName("deve persistir notificação e enviar email para PATIENT_CANCELLED")
        void deveProcessarPatientCancelled() {
            QueueEventDTO event = buildEvent("PATIENT_CANCELLED");

            notificationService.process(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).persist(captor.capture());
            Notification saved = captor.getValue();

            assertThat(saved.status).isEqualTo("ENVIADO");
            assertThat(saved.message).contains("Consulta Cardiológica");
        }

        @Test
        @DisplayName("deve definir status FALHA quando o envio de email lança exceção")
        void deveDefinirStatusFalhaQuandoEmailFalha() {
            QueueEventDTO event = buildEvent("PATIENT_CALLED");
            doThrow(new RuntimeException("SMTP indisponível"))
                    .when(emailService).send(anyString(), anyString(), anyString());

            notificationService.process(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).persist(captor.capture());
            assertThat(captor.getValue().status).isEqualTo("FALHA");
        }

        @Test
        @DisplayName("deve ignorar evento com tipo desconhecido sem persistir")
        void deveIgnorarEventoComTipoDesconhecido() {
            QueueEventDTO event = buildEvent("TIPO_INVALIDO");

            assertThatCode(() -> notificationService.process(event))
                    .doesNotThrowAnyException();

            verify(notificationRepository, never()).persist(any(Notification.class));
            verify(emailService, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("deve preencher sentAt com o timestamp do evento")
        void devePreencherSentAtComTimestampDoEvento() {
            LocalDateTime timestamp = LocalDateTime.of(2025, 3, 15, 9, 30);
            QueueEventDTO event = new QueueEventDTO(
                    "PATIENT_CALLED", UUID.randomUUID(),
                    "Maria", "maria@email.com",
                    "Exame de Sangue", "VERDE", timestamp);

            notificationService.process(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).persist(captor.capture());
            assertThat(captor.getValue().sentAt).isEqualTo(timestamp);
        }
    }

    // ── processSolicitacaoNegada() ──────────────────────────────────────────────

    @Nested
    @DisplayName("processSolicitacaoNegada()")
    class ProcessSolicitacaoNegada {

        @Test
        @DisplayName("deve persistir notificação com o motivo da negação")
        void devePersistirNotificacao() {
            SolicitacaoNegadaEventDTO event = new SolicitacaoNegadaEventDTO(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    "Sem indicação clínica", LocalDateTime.of(2026, 6, 12, 11, 30));

            notificationService.processSolicitacaoNegada(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).persist(captor.capture());
            Notification saved = captor.getValue();

            assertThat(saved.eventType).isEqualTo("SOLICITATION_DENIED");
            assertThat(saved.message).contains("Sem indicação clínica");
            assertThat(saved.sentAt).isEqualTo(LocalDateTime.of(2026, 6, 12, 11, 30));
            assertThat(saved.status).isEqualTo("ENVIADO");
        }
    }

    // ── processSolicitacaoDevolvida() ───────────────────────────────────────────

    @Nested
    @DisplayName("processSolicitacaoDevolvida()")
    class ProcessSolicitacaoDevolvida {

        @Test
        @DisplayName("deve persistir notificação com o motivo da devolução")
        void devePersistirNotificacao() {
            SolicitacaoDevolvidaEventDTO event = new SolicitacaoDevolvidaEventDTO(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    "Faltam dados do CID");

            notificationService.processSolicitacaoDevolvida(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).persist(captor.capture());
            Notification saved = captor.getValue();

            assertThat(saved.eventType).isEqualTo("SOLICITATION_DEVOLVED");
            assertThat(saved.message).contains("Faltam dados do CID");
            assertThat(saved.status).isEqualTo("ENVIADO");
        }
    }

    // ── processAppointmentConfirmed() ───────────────────────────────────────────

    @Nested
    @DisplayName("processAppointmentConfirmed()")
    class ProcessAppointmentConfirmed {

        @Test
        @DisplayName("deve persistir notificação com a data do agendamento")
        void devePersistirNotificacao() {
            LocalDateTime agendadoEm = LocalDateTime.of(2026, 7, 10, 8, 30);
            AppointmentConfirmedEventDTO event = new AppointmentConfirmedEventDTO(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), agendadoEm);

            notificationService.processAppointmentConfirmed(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).persist(captor.capture());
            Notification saved = captor.getValue();

            assertThat(saved.eventType).isEqualTo("APPOINTMENT_CONFIRMED");
            assertThat(saved.sentAt).isEqualTo(agendadoEm);
            assertThat(saved.status).isEqualTo("ENVIADO");
        }
    }

    // ── processAppointmentNoSlot() ───────────────────────────────────────────────

    @Nested
    @DisplayName("processAppointmentNoSlot()")
    class ProcessAppointmentNoSlot {

        @Test
        @DisplayName("deve persistir notificação informando ausência de vaga")
        void devePersistirNotificacao() {
            AppointmentNoSlotEventDTO event = new AppointmentNoSlotEventDTO(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            notificationService.processAppointmentNoSlot(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).persist(captor.capture());
            Notification saved = captor.getValue();

            assertThat(saved.eventType).isEqualTo("APPOINTMENT_NO_SLOT");
            assertThat(saved.status).isEqualTo("ENVIADO");
        }
    }
}
