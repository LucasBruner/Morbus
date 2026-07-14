package br.com.morbus.queueservice.infrastructure.messaging;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EDestino;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.infrastructure.messaging.DTO.QueueEventPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMqQueueEventPublisher")
class RabbitMqQueueEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitMqQueueEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RabbitMqQueueEventPublisher(rabbitTemplate);
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private QueueEntry buildEntry() {
        Patient patient = Patient.builder()
                .id(UUID.randomUUID())
                .nome("Maria")
                .sobrenome("Oliveira")
                .cpf("123.456.789-00")
                .dataNascimento(LocalDate.of(1980, 6, 10))
                .grupoLegal(EPriorityGroup.GERAL)
                .contato("maria@email.com")
                .build();

        Procedure procedure = Procedure.builder()
                .id(UUID.randomUUID())
                .coProcedimento("0301010072")
                .noProcedimento("Consulta de Cardiologia")
                .build();

        return QueueEntry.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .procedure(procedure)
                .riskColor(ERiskColor.AMARELO)
                .tipoFila(EDestino.FILA_REGULADA)
                .preferredUnitId(UUID.randomUUID())
                .queueStatus(EQueueStatus.AGUARDANDO)
                .registeredAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    private QueueEventPayload capturePayload() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), captor.capture());
        return (QueueEventPayload) captor.getValue();
    }

    private QueueEventPayload capturePayload(String exchange, String routingKey) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq(exchange), eq(routingKey), captor.capture());
        return (QueueEventPayload) captor.getValue();
    }

    // ── publishPatientRegistered ──────────────────────────────────────────────

    @Nested
    @DisplayName("publishPatientRegistered")
    class PublishPatientRegistered {

        @Test
        @DisplayName("deve publicar no exchange e routing key corretos")
        void devePublicarNoExchangeERkCorretos() {
            QueueEntry entry = buildEntry();
            publisher.publishPatientRegistered(entry);

            verify(rabbitTemplate).convertAndSend(
                    eq("sus.queue.exchange"), eq("patient.registered"),
                    any(QueueEventPayload.class)
            );
        }

        @Test
        @DisplayName("deve enviar eventType PATIENT_REGISTERED")
        void deveEnviarEventTypeCorreto() {
            QueueEntry entry = buildEntry();
            publisher.publishPatientRegistered(entry);

            QueueEventPayload payload = capturePayload("sus.queue.exchange", "patient.registered");
            assertThat(payload.eventType()).isEqualTo("PATIENT_REGISTERED");
        }

        @Test
        @DisplayName("deve preencher os campos do payload corretamente")
        void devePreencherPayload() {
            QueueEntry entry = buildEntry();
            Instant antes = Instant.now();
            publisher.publishPatientRegistered(entry);
            Instant depois = Instant.now();

            QueueEventPayload payload = capturePayload("sus.queue.exchange", "patient.registered");

            assertThat(payload.queueEntryId()).isEqualTo(entry.getId());
            assertThat(payload.patientId()).isEqualTo(entry.getPatient().getId());
            assertThat(payload.patientName()).isEqualTo("Maria Oliveira");
            assertThat(payload.patientContact()).isEqualTo("maria@email.com");
            assertThat(payload.procedureName()).isEqualTo("Consulta de Cardiologia");
            assertThat(payload.procedureId()).isEqualTo(entry.getProcedure().getId());
            assertThat(payload.preferredUnitId()).isEqualTo(entry.getPreferredUnitId());
            assertThat(payload.riskColor()).isEqualTo(ERiskColor.AMARELO);
            assertThat(payload.tipoFila()).isEqualTo(EDestino.FILA_REGULADA);
            assertThat(payload.timestamp()).isAfterOrEqualTo(antes).isBeforeOrEqualTo(depois);
            assertThat(payload.motivoCancelamento()).isNull();
        }
    }

    // ── publishPatientCalled ──────────────────────────────────────────────────

    @Nested
    @DisplayName("publishPatientCalled")
    class PublishPatientCalled {

        @Test
        @DisplayName("deve publicar no exchange e routing key corretos")
        void devePublicarNoExchangeERkCorretos() {
            QueueEntry entry = buildEntry();
            publisher.publishPatientCalled(entry);

            verify(rabbitTemplate).convertAndSend(
                    eq("sus.queue.exchange"), eq("patient.called"),
                    any(QueueEventPayload.class)
            );
        }

        @Test
        @DisplayName("deve enviar eventType PATIENT_CALLED")
        void deveEnviarEventTypeCorreto() {
            QueueEntry entry = buildEntry();
            publisher.publishPatientCalled(entry);

            QueueEventPayload payload = capturePayload("sus.queue.exchange", "patient.called");
            assertThat(payload.eventType()).isEqualTo("PATIENT_CALLED");
        }

        @Test
        @DisplayName("deve usar noProcedimento como procedureName")
        void deveUsarNomeDoProcedimento() {
            QueueEntry entry = buildEntry();
            publisher.publishPatientCalled(entry);

            QueueEventPayload payload = capturePayload("sus.queue.exchange", "patient.called");
            assertThat(payload.procedureName()).isEqualTo("Consulta de Cardiologia");
        }

        @Test
        @DisplayName("timestamp deve refletir o momento do evento, não o registeredAt")
        void timestampDeveSerMomentoDoEvento() {
            QueueEntry entry = buildEntry();
            Instant antes = Instant.now();
            publisher.publishPatientCalled(entry);
            Instant depois = Instant.now();

            QueueEventPayload payload = capturePayload("sus.queue.exchange", "patient.called");
            assertThat(payload.timestamp())
                    .isAfterOrEqualTo(antes)
                    .isBeforeOrEqualTo(depois);
        }

        @Test
        @DisplayName("deve incluir patientId, procedureId, preferredUnitId e tipoFila " +
                "para permitir a alocação de slot no agendamento-service")
        void deveIncluirCamposNecessariosParaAlocacaoDeSlot() {
            QueueEntry entry = buildEntry();
            publisher.publishPatientCalled(entry);

            QueueEventPayload payload = capturePayload("sus.queue.exchange", "patient.called");
            assertThat(payload.patientId()).isEqualTo(entry.getPatient().getId());
            assertThat(payload.procedureId()).isEqualTo(entry.getProcedure().getId());
            assertThat(payload.preferredUnitId()).isEqualTo(entry.getPreferredUnitId());
            assertThat(payload.tipoFila()).isEqualTo(entry.getTipoFila());
        }
    }

    // ── publishPriorityUpdated ────────────────────────────────────────────────

    @Nested
    @DisplayName("publishPriorityUpdated")
    class PublishPriorityUpdated {

        @Test
        @DisplayName("deve publicar no exchange e routing key corretos")
        void devePublicarNoExchangeERkCorretos() {
            QueueEntry entry = buildEntry();
            publisher.publishPriorityUpdated(entry);

            verify(rabbitTemplate).convertAndSend(
                    eq("sus.queue.exchange"), eq("priority.updated"),
                    any(QueueEventPayload.class)
            );
        }

        @Test
        @DisplayName("deve enviar eventType PRIORITY_UPDATED")
        void deveEnviarEventTypeCorreto() {
            QueueEntry entry = buildEntry();
            publisher.publishPriorityUpdated(entry);

            QueueEventPayload payload = capturePayload("sus.queue.exchange", "priority.updated");
            assertThat(payload.eventType()).isEqualTo("PRIORITY_UPDATED");
        }

        @Test
        @DisplayName("deve preencher queueEntryId e riskColor corretamente")
        void devePreencherCamposBasicos() {
            QueueEntry entry = buildEntry();
            publisher.publishPriorityUpdated(entry);

            QueueEventPayload payload = capturePayload("sus.queue.exchange", "priority.updated");
            assertThat(payload.queueEntryId()).isEqualTo(entry.getId());
            assertThat(payload.riskColor()).isEqualTo(ERiskColor.AMARELO);
        }
    }

    // ── publishPatientCancelled ───────────────────────────────────────────────

    @Nested
    @DisplayName("publishPatientCancelled")
    class PublishPatientCancelled {

        @Test
        @DisplayName("deve publicar no exchange e routing key corretos")
        void devePublicarNoExchangeERkCorretos() {
            QueueEntry entry = buildEntry();
            publisher.publishPatientCancelled(entry, "Paciente desistiu");

            verify(rabbitTemplate).convertAndSend(
                    eq("sus.queue.exchange"), eq("patient.cancelled"),
                    any(QueueEventPayload.class)
            );
        }

        @Test
        @DisplayName("deve enviar eventType PATIENT_CANCELLED")
        void deveEnviarEventTypeCorreto() {
            QueueEntry entry = buildEntry();
            publisher.publishPatientCancelled(entry, "Motivo qualquer");

            QueueEventPayload payload = capturePayload("sus.queue.exchange", "patient.cancelled");
            assertThat(payload.eventType()).isEqualTo("PATIENT_CANCELLED");
        }

        @Test
        @DisplayName("deve incluir o motivo no payload")
        void deveIncluirMotivo() {
            QueueEntry entry = buildEntry();
            String motivo = "Paciente não compareceu";
            publisher.publishPatientCancelled(entry, motivo);

            QueueEventPayload payload = capturePayload("sus.queue.exchange", "patient.cancelled");
            assertThat(payload.motivoCancelamento()).isEqualTo(motivo);
        }

        @Test
        @DisplayName("deve aceitar motivo nulo")
        void deveAceitarMotivoNulo() {
            QueueEntry entry = buildEntry();
            publisher.publishPatientCancelled(entry, null);

            QueueEventPayload payload = capturePayload("sus.queue.exchange", "patient.cancelled");
            assertThat(payload.motivoCancelamento()).isNull();
        }

        @Test
        @DisplayName("deve preencher patientName com nome e sobrenome")
        void devePreencherPatientName() {
            QueueEntry entry = buildEntry();
            publisher.publishPatientCancelled(entry, "Motivo");

            QueueEventPayload payload = capturePayload("sus.queue.exchange", "patient.cancelled");
            assertThat(payload.patientName()).isEqualTo("Maria Oliveira");
        }
    }
}
