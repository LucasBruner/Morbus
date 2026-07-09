package br.com.morbus.queueservice.integration;

import br.com.morbus.queueservice.domain.enums.EDestino;
import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.usecase.CallNextPatient;
import br.com.morbus.queueservice.domain.usecase.RegisterPatient;
import br.com.morbus.queueservice.domain.usecase.RegisterPatientInQueue;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterPatientDTO;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterQueueRequestDTO;
import br.com.morbus.queueservice.infrastructure.database.entity.ProcedureEntity;
import br.com.morbus.queueservice.infrastructure.database.repository.PatientJpaRepository;
import br.com.morbus.queueservice.infrastructure.database.repository.ProcedureJpaRepository;
import br.com.morbus.queueservice.infrastructure.database.repository.QueueEntryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para verificação da ordenação da fila com PostgreSQL real via Testcontainers.
 *
 * <p>Critérios cobertos:</p>
 * <ul>
 *   <li>FILA_REGULADA precede FILA_ESPERA independentemente de cor ou tempo de espera</li>
 *   <li>Ordenação correta com múltiplos pacientes misturando FILA_REGULADA/FILA_ESPERA, cores e grupos</li>
 *   <li>Paciente com 60+ anos e grupoLegal=GERAL é posicionado antes de GERAL mais novo (IDOSO automático)</li>
 * </ul>
 */
@DisplayName("IT — Ordenação da fila")
class QueueOrderingIT extends AbstractContainerIT {

    // RabbitTemplate mockado para que os eventos não precisem de listener real no contexto
    @MockitoBean
    RabbitTemplate rabbitTemplate;

    @Autowired RegisterPatient registerPatient;
    @Autowired RegisterPatientInQueue registerPatientInQueue;
    @Autowired CallNextPatient callNextPatient;
    @Autowired IQueueEntryRepository queueEntryRepository;
    @Autowired ProcedureJpaRepository procedureJpaRepository;
    @Autowired PatientJpaRepository patientJpaRepository;
    @Autowired QueueEntryJpaRepository queueEntryJpaRepository;

    private UUID procedureId;

    // Intervalo mínimo entre inserções para garantir registeredAt distintos (mesma
    // convenção usada em QueueFlowIntegrationTest.OrdenacaoPorRegisteredAt).
    private static final long REGISTERED_AT_GAP_MS = 20;
    private static final long ARRIVAL_ORDER_GAP_MS = 50;

    @BeforeEach
    void cleanAndSetup() {
        queueEntryJpaRepository.deleteAll();
        patientJpaRepository.deleteAll();
        procedureId = procedureJpaRepository.findAll().stream()
                .filter(p -> p.getIdadeMinima() == 0 && p.getIdadeMaxima() >= 60)
                .findFirst()
                .map(ProcedureEntity::getId)
                .orElseThrow(() -> new IllegalStateException("Nenhum procedimento geral disponível"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private QueueEntry enqueue(String cpf, String nome, LocalDate dob,
                               EPriorityGroup grupo, ERiskColor cor, EDestino tipoFila)
            throws InterruptedException {
        var patient = registerPatient.run(new RegisterPatientDTO(
                cpf, null, nome, "Sobrenome", dob, EGender.MASCULINO, null, grupo));
        Thread.sleep(REGISTERED_AT_GAP_MS); // garante timestamps distintos
        return registerPatientInQueue.execute(
                new RegisterQueueRequestDTO(patient.getId(), procedureId, cor, tipoFila));
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("FILA_REGULADA precede FILA_ESPERA")
    class FilaReguladaPrecedeFilaEspera {

        @Test
        @DisplayName("FILA_REGULADA com AZUL deve ser chamado antes de FILA_ESPERA com AZUL chegado antes")
        void filaRegulada_antesDeFilaEspera_independenteDeCor() throws InterruptedException {
            // FILA_ESPERA chega primeiro (timestamp menor)
            enqueue("111.111.111-91", "Espera", LocalDate.of(1985, 1, 1),
                    EPriorityGroup.GERAL, ERiskColor.AZUL, EDestino.FILA_ESPERA);
            Thread.sleep(ARRIVAL_ORDER_GAP_MS);
            // FILA_REGULADA chega depois (timestamp maior), mas deve ter prioridade
            enqueue("222.222.222-00", "Regulada", LocalDate.of(1985, 1, 1),
                    EPriorityGroup.GERAL, ERiskColor.AZUL, EDestino.FILA_REGULADA);

            List<QueueEntry> ordered = queueEntryRepository.findAllOrderedByPriority();

            assertThat(ordered).hasSize(2);
            assertThat(ordered.get(0).getTipoFila()).isEqualTo(EDestino.FILA_REGULADA);
            assertThat(ordered.get(1).getTipoFila()).isEqualTo(EDestino.FILA_ESPERA);
        }

        @Test
        @DisplayName("FILA_REGULADA VERMELHO precede FILA_ESPERA em qualquer ordem de chegada")
        void filaRegulada_vermelho_sempresAntesDeFilaEspera() throws InterruptedException {
            enqueue("333.333.333-09", "EsperaAzul", LocalDate.of(1990, 3, 3),
                    EPriorityGroup.GERAL, ERiskColor.AZUL, EDestino.FILA_ESPERA);
            Thread.sleep(ARRIVAL_ORDER_GAP_MS);
            enqueue("444.444.444-08", "ReguladaVermelho", LocalDate.of(1990, 3, 3),
                    EPriorityGroup.GERAL, ERiskColor.VERMELHO, EDestino.FILA_REGULADA);

            QueueEntry next = callNextPatient.run();

            assertThat(next.getPatient().getNome()).isEqualTo("ReguladaVermelho");
            assertThat(next.getTipoFila()).isEqualTo(EDestino.FILA_REGULADA);
            assertThat(next.getQueueStatus()).isEqualTo(EQueueStatus.AGENDADO);
        }

        @Test
        @DisplayName("Múltiplos pacientes misturando tipos — ordenação completa correta")
        void ordenacaoCompleta_multiplostipos() throws InterruptedException {
            // 4 pacientes em ordem aleatória de chegada
            enqueue("555.555.555-07", "EsperaA", LocalDate.of(1985, 1, 1),
                    EPriorityGroup.GERAL, ERiskColor.AZUL, EDestino.FILA_ESPERA);
            enqueue("666.666.666-06", "ReguladaVerde", LocalDate.of(1985, 1, 1),
                    EPriorityGroup.GERAL, ERiskColor.VERDE, EDestino.FILA_REGULADA);
            enqueue("777.777.777-05", "EsperaB", LocalDate.of(1985, 1, 1),
                    EPriorityGroup.GERAL, ERiskColor.AZUL, EDestino.FILA_ESPERA);
            enqueue("888.888.888-04", "ReguladaAmarelo", LocalDate.of(1985, 1, 1),
                    EPriorityGroup.GERAL, ERiskColor.AMARELO, EDestino.FILA_REGULADA);

            List<QueueEntry> ordered = queueEntryRepository.findAllOrderedByPriority();

            // FILA_REGULADA nas primeiras posições
            assertThat(ordered.get(0).getTipoFila()).isEqualTo(EDestino.FILA_REGULADA);
            assertThat(ordered.get(1).getTipoFila()).isEqualTo(EDestino.FILA_REGULADA);
            // FILA_ESPERA nas últimas
            assertThat(ordered.get(2).getTipoFila()).isEqualTo(EDestino.FILA_ESPERA);
            assertThat(ordered.get(3).getTipoFila()).isEqualTo(EDestino.FILA_ESPERA);
            // Dentro de FILA_REGULADA: AMARELO antes de VERDE
            assertThat(ordered.get(0).getRiskColor()).isEqualTo(ERiskColor.AMARELO);
            assertThat(ordered.get(1).getRiskColor()).isEqualTo(ERiskColor.VERDE);
        }
    }

    @Nested
    @DisplayName("IDOSO automático — paciente 60+ anos com grupoLegal=GERAL")
    class IdosoAutomatico {

        @Test
        @DisplayName("Paciente 60+ anos com GERAL é posicionado antes de GERAL mais novo")
        void idoso_geral_antesDeGeral_maisNovo() throws InterruptedException {
            // Jovem GERAL chega primeiro
            enqueue("111.444.777-35", "Jovem", LocalDate.of(1990, 6, 1),
                    EPriorityGroup.GERAL, ERiskColor.AMARELO, EDestino.FILA_REGULADA);
            // Idoso (65 anos) cadastrado como GERAL chega depois
            enqueue("222.555.888-35", "Idoso", LocalDate.now().minusYears(65),
                    EPriorityGroup.GERAL, ERiskColor.AMARELO, EDestino.FILA_REGULADA);

            List<QueueEntry> ordered = queueEntryRepository.findAllOrderedByPriority();

            // IDOSO automático precede GERAL mesmo chegando depois
            assertThat(ordered.get(0).getPatient().getNome()).isEqualTo("Idoso");
            assertThat(ordered.get(0).getPatient().getGrupoLegal()).isEqualTo(EPriorityGroup.IDOSO);
            assertThat(ordered.get(1).getPatient().getNome()).isEqualTo("Jovem");
        }

        @Test
        @DisplayName("callNext chama o idoso automático antes do jovem GERAL")
        void callNext_chamaIdosoAutomatico_antesDeJovem() throws InterruptedException {
            enqueue("333.666.999-35", "JovemGeral", LocalDate.of(1992, 3, 10),
                    EPriorityGroup.GERAL, ERiskColor.AMARELO, EDestino.FILA_REGULADA);
            enqueue("444.777.111-35", "IdosoGeral", LocalDate.now().minusYears(70),
                    EPriorityGroup.GERAL, ERiskColor.AMARELO, EDestino.FILA_REGULADA);

            QueueEntry chamado = callNextPatient.run();

            assertThat(chamado.getPatient().getNome()).isEqualTo("IdosoGeral");
            assertThat(chamado.getPatient().getGrupoLegal()).isEqualTo(EPriorityGroup.IDOSO);
        }
    }
}
