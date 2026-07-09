package br.com.morbus.queueservice.integration;

import br.com.morbus.queueservice.domain.enums.EDestino;
import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.usecase.CallNextPatient;
import br.com.morbus.queueservice.domain.usecase.RegisterPatient;
import br.com.morbus.queueservice.domain.usecase.RegisterPatientInQueue;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterPatientDTO;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterQueueRequestDTO;
import br.com.morbus.queueservice.infrastructure.database.entity.ProcedureEntity;
import br.com.morbus.queueservice.infrastructure.database.repository.PatientJpaRepository;
import br.com.morbus.queueservice.infrastructure.database.repository.ProcedureJpaRepository;
import br.com.morbus.queueservice.infrastructure.database.repository.QueueEntryJpaRepository;
import br.com.morbus.queueservice.infrastructure.messaging.DTO.QueueEventPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração de publicação de eventos RabbitMQ com broker real via Testcontainers.
 *
 * <p>Critérios cobertos (fluxo ponta a ponta):</p>
 * <ul>
 *   <li>Paciente inserido na fila publica evento {@code patient.registered} no RabbitMQ</li>
 *   <li>{@code callNext} publica evento {@code patient.called} no RabbitMQ</li>
 * </ul>
 */
@DisplayName("IT — Publicação de eventos RabbitMQ")
class QueueRabbitMQIT extends AbstractContainerIT {

    private static final String TEST_EXCHANGE = "sus.queue.exchange";
    private static final long RECEIVE_TIMEOUT_MS = 5_000L;

    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired AmqpAdmin amqpAdmin;
    @Autowired ObjectMapper objectMapper;

    @Autowired RegisterPatient registerPatient;
    @Autowired RegisterPatientInQueue registerPatientInQueue;
    @Autowired CallNextPatient callNextPatient;
    @Autowired ProcedureJpaRepository procedureJpaRepository;
    @Autowired PatientJpaRepository patientJpaRepository;
    @Autowired QueueEntryJpaRepository queueEntryJpaRepository;

    private UUID procedureId;

    private static final String QUEUE_PATIENT_REGISTERED  = "it.test.patient.registered";
    private static final String QUEUE_PATIENT_CALLED      = "it.test.patient.called";

    @BeforeEach
    void cleanAndSetup() {
        queueEntryJpaRepository.deleteAll();
        patientJpaRepository.deleteAll();

        procedureId = procedureJpaRepository.findAll().stream()
                .filter(p -> p.getIdadeMinima() == 0)
                .findFirst()
                .map(ProcedureEntity::getId)
                .orElseThrow();

        // Declara filas de teste transientes para capturar os eventos publicados
        declareTestQueue(QUEUE_PATIENT_REGISTERED, "patient.registered");
        declareTestQueue(QUEUE_PATIENT_CALLED,     "patient.called");
    }

    private void declareTestQueue(String queueName, String routingKey) {
        Queue q = new Queue(queueName, false, true, true);
        amqpAdmin.declareQueue(q);
        amqpAdmin.declareBinding(
                BindingBuilder.bind(q)
                        .to(new DirectExchange(TEST_EXCHANGE))
                        .with(routingKey));
        // Descarta qualquer mensagem remanescente de um teste anterior que tenha
        // publicado mas não consumido (ex.: falha no meio do teste), evitando que
        // ela seja entregue ao próximo método da mesma classe/container.
        amqpAdmin.purgeQueue(queueName, false);
    }

    private UUID registerAndEnqueue(String cpf, String nome, ERiskColor cor, EDestino tipoFila) {
        var patient = registerPatient.run(new RegisterPatientDTO(
                cpf, null, nome, "Sobrenome", LocalDate.of(1985, 1, 1),
                EGender.MASCULINO, "email@it.com", EPriorityGroup.GERAL));
        registerPatientInQueue.execute(
                new RegisterQueueRequestDTO(patient.getId(), procedureId, cor, tipoFila));
        return patient.getId();
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("patient.registered")
    class PatientRegisteredEvent {

        @Test
        @DisplayName("Inserir paciente na fila publica patient.registered no RabbitMQ")
        void enqueue_publicaPatientRegistered() throws Exception {
            registerAndEnqueue("111.444.777-35", "Evento", ERiskColor.AMARELO, EDestino.FILA_REGULADA);

            Message msg = rabbitTemplate.receive(QUEUE_PATIENT_REGISTERED, RECEIVE_TIMEOUT_MS);

            assertThat(msg).as("Evento patient.registered não chegou no timeout").isNotNull();
            QueueEventPayload payload = objectMapper.readValue(msg.getBody(), QueueEventPayload.class);
            assertThat(payload.eventType()).isEqualTo("PATIENT_REGISTERED");
            assertThat(payload.patientName()).contains("Evento");
        }

        @Test
        @DisplayName("Dois pacientes enfileirados publicam dois eventos patient.registered")
        void doisPacientes_publicaDoisEventos() throws Exception {
            registerAndEnqueue("222.555.888-35", "Um", ERiskColor.AZUL, EDestino.FILA_REGULADA);
            registerAndEnqueue("333.666.999-35", "Dois", ERiskColor.VERDE, EDestino.FILA_REGULADA);

            Message msg1 = rabbitTemplate.receive(QUEUE_PATIENT_REGISTERED, RECEIVE_TIMEOUT_MS);
            Message msg2 = rabbitTemplate.receive(QUEUE_PATIENT_REGISTERED, RECEIVE_TIMEOUT_MS);

            assertThat(msg1).as("Primeiro evento não chegou").isNotNull();
            assertThat(msg2).as("Segundo evento não chegou").isNotNull();
        }
    }

    @Nested
    @DisplayName("patient.called")
    class PatientCalledEvent {

        @Test
        @DisplayName("callNext publica patient.called com status AGENDADO")
        void callNext_publicaPatientCalled() throws Exception {
            registerAndEnqueue("444.777.111-35", "Chamado", ERiskColor.VERMELHO, EDestino.FILA_REGULADA);
            // Consome o patient.registered para limpar a fila de teste
            rabbitTemplate.receive(QUEUE_PATIENT_REGISTERED, RECEIVE_TIMEOUT_MS);

            callNextPatient.run();

            Message msg = rabbitTemplate.receive(QUEUE_PATIENT_CALLED, RECEIVE_TIMEOUT_MS);

            assertThat(msg).as("Evento patient.called não chegou no timeout").isNotNull();
            QueueEventPayload payload = objectMapper.readValue(msg.getBody(), QueueEventPayload.class);
            assertThat(payload.eventType()).isEqualTo("PATIENT_CALLED");
            assertThat(payload.patientName()).contains("Chamado");
        }
    }
}
