package br.com.morbus.queueservice.integration;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EDestino;
import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.usecase.CallNextPatient;
import br.com.morbus.queueservice.domain.usecase.RegisterPatient;
import br.com.morbus.queueservice.domain.usecase.RegisterPatientInQueue;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterPatientDTO;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterQueueRequestDTO;
import br.com.morbus.queueservice.infrastructure.database.entity.ProcedureEntity;
import br.com.morbus.queueservice.infrastructure.database.repository.ProcedureJpaRepository;
import br.com.morbus.queueservice.infrastructure.messaging.consumer.AppointmentExpiredConsumer;
import br.com.morbus.queueservice.infrastructure.messaging.consumer.AppointmentExpiredEvent;
import br.com.morbus.queueservice.infrastructure.messaging.consumer.PatientNoShowConsumer;
import br.com.morbus.queueservice.infrastructure.messaging.consumer.PatientNoShowEvent;
import br.com.morbus.queueservice.infrastructure.messaging.consumer.SolicitationApprovedConsumer;
import br.com.morbus.queueservice.infrastructure.messaging.consumer.SolicitationApprovedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Testes de integração dos consumers RabbitMQ que fecham o fluxo de
 * regulacao-service/agendamento-service com o queue-service (issue #141).
 *
 * <p>Contexto Spring real com banco H2 (RabbitTemplate mockado, sem broker real),
 * chamando diretamente o método anotado com @RabbitListener para validar a
 * fiação consumer -> use case -> repositório -> publisher.</p>
 */
@SpringBootTest
@Transactional
@DisplayName("Integração — Consumers de regulacao-service e agendamento-service")
class RegulacaoAgendamentoConsumerIntegrationTest {

    @MockitoBean
    RabbitTemplate rabbitTemplate;

    @MockitoBean
    IQueueEventPublisher eventPublisher;

    @Autowired SolicitationApprovedConsumer solicitationApprovedConsumer;
    @Autowired AppointmentExpiredConsumer appointmentExpiredConsumer;
    @Autowired PatientNoShowConsumer patientNoShowConsumer;

    @Autowired RegisterPatient registerPatient;
    @Autowired RegisterPatientInQueue registerPatientInQueue;
    @Autowired CallNextPatient callNextPatient;
    @Autowired IQueueEntryRepository queueEntryRepository;
    @Autowired ProcedureJpaRepository procedureJpaRepository;

    UUID procedureId;

    @BeforeEach
    void setUp() {
        ProcedureEntity proc = procedureJpaRepository.save(ProcedureEntity.builder()
                .coProcedimento("IT-CONSUMER-GERAL")
                .noProcedimento("Consulta Ambulatorial de Integração")
                .idadeMinima(0)
                .idadeMaxima(120)
                .build());
        procedureId = proc.getId();
    }

    private UUID registerPatientId(String cpf, String nome) {
        var patient = registerPatient.run(new RegisterPatientDTO(
                cpf, null, nome, "Sobrenome", LocalDate.of(1990, 1, 1),
                EGender.MASCULINO, null, EPriorityGroup.GERAL));
        return patient.getId();
    }

    // ── SolicitationApprovedConsumer ────────────────────────────────────────

    @Nested
    @DisplayName("SolicitationApprovedConsumer")
    class SolicitationApproved {

        @Test
        @DisplayName("FILA_REGULADA cria QueueEntry AGUARDANDO com a cor de risco do evento")
        void filaRegulada_criaEntradaComRiskColorDoEvento() {
            UUID patientId = registerPatientId("11111111111", "Ana");

            solicitationApprovedConsumer.onSolicitationApproved(new SolicitationApprovedEvent(
                    UUID.randomUUID(), patientId, procedureId, UUID.randomUUID(),
                    EDestino.FILA_REGULADA, ERiskColor.VERMELHO));

            var entries = queueEntryRepository.findByPatientAndStatusIn(
                    br.com.morbus.queueservice.domain.entity.Patient.builder().id(patientId).build(),
                    java.util.List.of(EQueueStatus.AGUARDANDO));

            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).getRiskColor()).isEqualTo(ERiskColor.VERMELHO);
            assertThat(entries.get(0).getQueueStatus()).isEqualTo(EQueueStatus.AGUARDANDO);
            verify(eventPublisher, times(1)).publishPatientRegistered(any(QueueEntry.class));
        }

        @Test
        @DisplayName("FILA_ESPERA força AZUL mesmo que o evento traga outra cor")
        void filaEspera_forcaAzul() {
            UUID patientId = registerPatientId("22222222222", "Beatriz");

            solicitationApprovedConsumer.onSolicitationApproved(new SolicitationApprovedEvent(
                    UUID.randomUUID(), patientId, procedureId, UUID.randomUUID(),
                    EDestino.FILA_ESPERA, ERiskColor.VERMELHO));

            var entries = queueEntryRepository.findByPatientAndStatusIn(
                    br.com.morbus.queueservice.domain.entity.Patient.builder().id(patientId).build(),
                    java.util.List.of(EQueueStatus.AGUARDANDO));

            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).getRiskColor()).isEqualTo(ERiskColor.AZUL);
        }
    }

    // ── AppointmentExpiredConsumer ──────────────────────────────────────────

    @Nested
    @DisplayName("AppointmentExpiredConsumer")
    class AppointmentExpired {

        @Test
        @DisplayName("reinsere o paciente AGENDADO de volta para AGUARDANDO e publica PATIENT_REINSTATED")
        void reinsereNaFila() {
            UUID patientId = registerPatientId("33333333333", "Carlos");
            registerPatientInQueue.execute(new RegisterQueueRequestDTO(patientId, procedureId, ERiskColor.AMARELO));
            QueueEntry chamado = callNextPatient.run();
            assertThat(chamado.getQueueStatus()).isEqualTo(EQueueStatus.AGENDADO);

            appointmentExpiredConsumer.onAppointmentExpired(
                    new AppointmentExpiredEvent(chamado.getId(), patientId, "Consulta Ambulatorial de Integração"));

            QueueEntry reinstated = queueEntryRepository.findById(chamado.getId()).orElseThrow();
            assertThat(reinstated.getQueueStatus()).isEqualTo(EQueueStatus.AGUARDANDO);
            verify(eventPublisher, times(1)).publishPatientReinstated(any(QueueEntry.class));
        }
    }

    // ── PatientNoShowConsumer ───────────────────────────────────────────────

    @Nested
    @DisplayName("PatientNoShowConsumer")
    class PatientNoShow {

        @Test
        @DisplayName("reinsere o paciente AGENDADO de volta para AGUARDANDO e publica PATIENT_REINSTATED")
        void reinsereNaFila() {
            UUID patientId = registerPatientId("44444444444", "Daniela");
            registerPatientInQueue.execute(new RegisterQueueRequestDTO(patientId, procedureId, ERiskColor.VERDE));
            QueueEntry chamado = callNextPatient.run();
            assertThat(chamado.getQueueStatus()).isEqualTo(EQueueStatus.AGENDADO);

            patientNoShowConsumer.onPatientNoShow(new PatientNoShowEvent(chamado.getId(), patientId));

            QueueEntry reinstated = queueEntryRepository.findById(chamado.getId()).orElseThrow();
            assertThat(reinstated.getQueueStatus()).isEqualTo(EQueueStatus.AGUARDANDO);
            verify(eventPublisher, times(1)).publishPatientReinstated(any(QueueEntry.class));
        }
    }
}
