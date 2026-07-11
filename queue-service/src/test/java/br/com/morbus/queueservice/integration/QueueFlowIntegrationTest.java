package br.com.morbus.queueservice.integration;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.usecase.CallNextPatient;
import br.com.morbus.queueservice.domain.usecase.RegisterPatient;
import br.com.morbus.queueservice.domain.usecase.RegisterPatientInQueue;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterPatientDTO;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterQueueRequestDTO;
import br.com.morbus.queueservice.infrastructure.database.entity.PatientEntity;
import br.com.morbus.queueservice.infrastructure.database.entity.ProcedureEntity;
import br.com.morbus.queueservice.infrastructure.database.repository.PatientJpaRepository;
import br.com.morbus.queueservice.infrastructure.database.repository.ProcedureJpaRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Testes de integração do fluxo completo da fila ambulatorial.
 *
 * <p>Contexto Spring real com banco H2 em memória (CA-01). RabbitMQ substituído
 * por mocks — sem dependências externas (CA-05).</p>
 *
 * <p>Cobre os seguintes critérios de aceite:</p>
 * <ul>
 *   <li>CA-01 — @SpringBootTest + H2, sem PostgreSQL externo</li>
 *   <li>CA-02 — fluxo completo: 3 pacientes → callNext → maior prioridade retornado</li>
 *   <li>CA-03 — ordenação: riskColor ASC → grupoLegal ASC → registeredAt ASC</li>
 *   <li>CA-04 — eventos verificados via @MockitoBean IQueueEventPublisher</li>
 *   <li>CA-05 — sem dependências externas; roda com 'mvn test'</li>
 * </ul>
 */
@SpringBootTest
@Transactional  // rollback automático após cada teste — banco sempre limpo
@DisplayName("Integração — Fluxo completo da fila ambulatorial")
class QueueFlowIntegrationTest {

    // CA-01: RabbitTemplate mockado para que o contexto suba sem broker real
    @MockitoBean
    RabbitTemplate rabbitTemplate;

    // CA-04: publisher mockado para verificar eventos sem RabbitMQ real
    @MockitoBean
    IQueueEventPublisher eventPublisher;

    @Autowired RegisterPatient registerPatient;
    @Autowired RegisterPatientInQueue registerPatientInQueue;
    @Autowired CallNextPatient callNextPatient;
    @Autowired IQueueEntryRepository queueEntryRepository;
    @Autowired ProcedureJpaRepository procedureJpaRepository;
    @Autowired PatientJpaRepository patientJpaRepository;

    /** ID do procedimento compartilhado entre os testes de cada método. */
    java.util.UUID procedureId;

    // ── Setup ────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        // Salva um procedimento elegível para todos (0–120 anos) direto no JPA,
        // pois não existe use case de cadastro de procedimento.
        ProcedureEntity proc = procedureJpaRepository.save(ProcedureEntity.builder()
                .coProcedimento("IT-CONSULTA-GERAL")
                .noProcedimento("Consulta Ambulatorial de Integração")
                .idadeMinima(0)
                .idadeMaxima(120)
                .build());
        procedureId = proc.getId();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Registra um paciente e o coloca na fila em uma única chamada.
     *
     * @param cpf      CPF único no escopo do teste (rollback garante isolamento)
     * @param nome     Nome do paciente (usado nas asserções)
     * @param dob      Data de nascimento (determina IDOSO automático se ≥60 anos)
     * @param grupo    Grupo de prioridade legal declarado
     * @param cor      Cor de risco clínico
     * @return         Entrada de fila criada
     */
    private QueueEntry enqueue(String cpf, String nome, LocalDate dob,
                                EPriorityGroup grupo, ERiskColor cor) {
        var patient = registerPatient.run(new RegisterPatientDTO(
                cpf, null, nome, "Sobrenome", dob, EGender.MASCULINO, null, grupo));
        return registerPatientInQueue.execute(
                new RegisterQueueRequestDTO(patient.getId(), procedureId, cor));
    }

    // ── CA-02: Fluxo completo ────────────────────────────────────────────────

    @Nested
    @DisplayName("CA-02 — Fluxo completo: 3 pacientes com prioridades distintas")
    class FluxoCompleto {

        @Test
        @DisplayName("callNext deve retornar VERMELHO+IDOSO antes de AMARELO+GERAL e VERDE+GESTANTE")
        void callNext_retornaMaiorPrioridade_vermelho_idoso() {
            // Inserir fora de ordem para garantir que a ordenação vem do algoritmo, não da inserção
            enqueue("33333333333", "Carlos",  LocalDate.of(1985, 1, 1),  EPriorityGroup.GERAL,    ERiskColor.VERDE);
            enqueue("22222222222", "Beatriz", LocalDate.of(1995, 6, 15), EPriorityGroup.GESTANTE, ERiskColor.AMARELO);
            enqueue("11111111111", "Ana",     LocalDate.of(1955, 3, 20), EPriorityGroup.IDOSO,    ERiskColor.VERMELHO);

            QueueEntry chamado = callNextPatient.run();

            assertThat(chamado.getPatient().getNome()).isEqualTo("Ana");
            assertThat(chamado.getRiskColor()).isEqualTo(ERiskColor.VERMELHO);
        }

        @Test
        @DisplayName("callNext deve alterar status para CHAMADO")
        void callNext_alteraStatusParaChamado() {
            enqueue("11111111111", "Ana", LocalDate.of(1955, 3, 20), EPriorityGroup.IDOSO, ERiskColor.VERMELHO);

            QueueEntry chamado = callNextPatient.run();

            assertThat(chamado.getQueueStatus().name()).isEqualTo("CHAMADO");
        }
    }

    // ── CA-03: Ordenação por riskColor ───────────────────────────────────────

    @Nested
    @DisplayName("CA-03 — Ordenação: riskColor ASC")
    class OrdenacaoPorRiskColor {

        @Test
        @DisplayName("VERMELHO → AMARELO → VERDE na fila ordenada")
        void listaOrdenada_riskColor_crescente() throws InterruptedException {
            // Inserir em ordem inversa para que seja o algoritmo que ordene, não o momento de inserção
            enqueue("33333333333", "Carlos", LocalDate.of(1985, 1, 1), EPriorityGroup.GERAL, ERiskColor.VERDE);
            Thread.sleep(20);
            enqueue("22222222222", "Beatriz", LocalDate.of(1990, 6, 1), EPriorityGroup.GERAL, ERiskColor.AMARELO);
            Thread.sleep(20);
            enqueue("11111111111", "Ana",    LocalDate.of(1980, 3, 1), EPriorityGroup.GERAL, ERiskColor.VERMELHO);

            List<QueueEntry> ordered = queueEntryRepository.findAllOrderedByPriority();

            assertThat(ordered).hasSize(3);
            assertThat(ordered.get(0).getRiskColor()).isEqualTo(ERiskColor.VERMELHO);
            assertThat(ordered.get(1).getRiskColor()).isEqualTo(ERiskColor.AMARELO);
            assertThat(ordered.get(2).getRiskColor()).isEqualTo(ERiskColor.VERDE);
        }
    }

    // ── CA-03: Ordenação por grupoLegal (desempate de riskColor igual) ────────

    @Nested
    @DisplayName("CA-03 — Ordenação: grupoLegal ASC dentro do mesmo riskColor")
    class OrdenacaoPorGrupoLegal {

        @Test
        @DisplayName("IDOSO → GESTANTE → GERAL quando riskColor é igual")
        void listaOrdenada_grupoLegal_crescente() throws InterruptedException {
            // Mesmo riskColor (AMARELO) para testar desempate por grupo
            enqueue("33333333333", "Carlos",  LocalDate.of(1990, 1, 1), EPriorityGroup.GERAL,    ERiskColor.AMARELO);
            Thread.sleep(20);
            enqueue("22222222222", "Beatriz", LocalDate.of(1995, 6, 1), EPriorityGroup.GESTANTE, ERiskColor.AMARELO);
            Thread.sleep(20);
            enqueue("11111111111", "Ana",     LocalDate.of(1955, 3, 1), EPriorityGroup.IDOSO,    ERiskColor.AMARELO);

            List<QueueEntry> ordered = queueEntryRepository.findAllOrderedByPriority();

            assertThat(ordered).hasSize(3);
            assertThat(ordered.get(0).getPatient().getGrupoLegal()).isEqualTo(EPriorityGroup.IDOSO);
            assertThat(ordered.get(1).getPatient().getGrupoLegal()).isEqualTo(EPriorityGroup.GESTANTE);
            assertThat(ordered.get(2).getPatient().getGrupoLegal()).isEqualTo(EPriorityGroup.GERAL);
        }

        @Test
        @DisplayName("Paciente ≥60 anos é promovido a IDOSO automaticamente, ganhando prioridade sobre GESTANTE")
        void idosoAutomatico_sobrescreveGeral_e_ganhaDeGestante() throws InterruptedException {
            // Antônio (65 anos) declarado como GERAL → deve ser promovido a IDOSO pelo PriorityCalculator
            enqueue("22222222222", "Beatriz", LocalDate.of(1995, 6, 1), EPriorityGroup.GESTANTE, ERiskColor.AMARELO);
            Thread.sleep(20);
            enqueue("11111111111", "Antônio", LocalDate.now().minusYears(65), EPriorityGroup.GERAL, ERiskColor.AMARELO);

            List<QueueEntry> ordered = queueEntryRepository.findAllOrderedByPriority();

            assertThat(ordered.get(0).getPatient().getNome()).isEqualTo("Antônio");
            assertThat(ordered.get(0).getPatient().getGrupoLegal()).isEqualTo(EPriorityGroup.IDOSO);
        }
    }

    // ── Ordenação dinâmica: paciente "envelhece" para IDOSO enquanto já está na fila ──

    @Nested
    @DisplayName("Ordenação dinâmica — paciente completa 60 anos com grupo_legal desatualizado (stale)")
    class OrdenacaoDinamicaPorIdade {

        /**
         * Simula a passagem do tempo: depois que o paciente já está na fila com
         * {@code grupo_legal = GERAL} (calculado corretamente quando ele tinha
         * menos de 60 anos), avança a data de nascimento via JPA direto — sem
         * passar por RegisterPatient/UpdatePatient/RegisterPatientInQueue, que
         * recalculariam e corrigiriam o snapshot. É exatamente o cenário real:
         * ninguém faz PATCH /patients/{id} nem re-registra o paciente em outra
         * fila só porque ele fez aniversário.
         */
        private void envelhecerSemAtualizarSnapshot(java.util.UUID patientId, LocalDate novaDataNascimento) {
            PatientEntity entity = patientJpaRepository.findById(patientId).orElseThrow();
            entity.setDataNascimento(novaDataNascimento);
            patientJpaRepository.save(entity); // grupoLegal não é tocado — continua GERAL
        }

        @Test
        @DisplayName("Paciente que completou 60 anos na fila é ordenado como IDOSO mesmo com grupo_legal=GERAL no banco")
        void completaSessentaAnosNaFila_aindaAssimOrdenaComoIdoso() throws InterruptedException {
            // Beatriz: GESTANTE de verdade, cadastro em dia
            enqueue("22222222222", "Beatriz", LocalDate.of(1995, 6, 1), EPriorityGroup.GESTANTE, ERiskColor.AMARELO);
            Thread.sleep(20);

            // Roberto: registrado com 59 anos (GERAL corretamente calculado e salvo)
            QueueEntry robertoEntry = enqueue("11111111111", "Roberto",
                    LocalDate.now().minusYears(59), EPriorityGroup.GERAL, ERiskColor.AMARELO);
            assertThat(robertoEntry.getPatient().getGrupoLegal()).isEqualTo(EPriorityGroup.GERAL);

            // Tempo passa, Roberto completa 60+ anos — ninguém atualiza o cadastro dele
            envelhecerSemAtualizarSnapshot(robertoEntry.getPatient().getId(), LocalDate.now().minusYears(65));

            List<QueueEntry> ordered = queueEntryRepository.findAllOrderedByPriority();

            // A ordenação deve tratar Roberto como IDOSO (idade recalculada ao vivo), à frente de
            // GESTANTE, mesmo com patients.grupo_legal ainda gravado como GERAL no banco.
            assertThat(ordered.get(0).getPatient().getNome()).isEqualTo("Roberto");
            assertThat(ordered.get(0).getPatient().getGrupoLegal()).isEqualTo(EPriorityGroup.GERAL); // snapshot intacto, propositalmente stale
            assertThat(ordered.get(1).getPatient().getNome()).isEqualTo("Beatriz");
        }

        @Test
        @DisplayName("callNext chama o paciente que envelheceu na fila como se fosse IDOSO")
        void callNext_respeitaIdadeDinamica_apesarDoSnapshotDesatualizado() throws InterruptedException {
            enqueue("22222222222", "Beatriz", LocalDate.of(1995, 6, 1), EPriorityGroup.GESTANTE, ERiskColor.AMARELO);
            Thread.sleep(20);

            QueueEntry robertoEntry = enqueue("11111111111", "Roberto",
                    LocalDate.now().minusYears(59), EPriorityGroup.GERAL, ERiskColor.AMARELO);
            envelhecerSemAtualizarSnapshot(robertoEntry.getPatient().getId(), LocalDate.now().minusYears(70));

            QueueEntry chamado = callNextPatient.run();

            assertThat(chamado.getPatient().getNome()).isEqualTo("Roberto");
        }
    }

    // ── CA-03: Ordenação por registeredAt (desempate final) ─────────────────

    @Nested
    @DisplayName("CA-03 — Ordenação: registeredAt ASC quando riskColor e grupoLegal são iguais")
    class OrdenacaoPorRegisteredAt {

        @Test
        @DisplayName("Primeiro registrado aparece antes do segundo quando todos os demais critérios são iguais")
        void listaOrdenada_registeredAt_crescente() throws InterruptedException {
            enqueue("11111111111", "Primeiro", LocalDate.of(1990, 1, 1), EPriorityGroup.GERAL, ERiskColor.VERDE);
            Thread.sleep(30);  // garante registeredAt distintos
            enqueue("22222222222", "Segundo",  LocalDate.of(1990, 6, 1), EPriorityGroup.GERAL, ERiskColor.VERDE);

            List<QueueEntry> ordered = queueEntryRepository.findAllOrderedByPriority();

            assertThat(ordered).hasSize(2);
            assertThat(ordered.get(0).getPatient().getNome()).isEqualTo("Primeiro");
            assertThat(ordered.get(1).getPatient().getNome()).isEqualTo("Segundo");
        }
    }

    // ── CA-04: Verificação de eventos ────────────────────────────────────────

    @Nested
    @DisplayName("CA-04 — Eventos publicados via IQueueEventPublisher")
    class PublicacaoDeEventos {

        @Test
        @DisplayName("registerPatientInQueue publica exatamente um evento patient.registered")
        void register_publicaEventoPatientRegistered() {
            enqueue("11111111111", "Ana", LocalDate.of(1980, 3, 1), EPriorityGroup.GERAL, ERiskColor.AZUL);

            verify(eventPublisher, times(1)).publishPatientRegistered(any(QueueEntry.class));
        }

        @Test
        @DisplayName("callNext publica exatamente um evento patient.called com a entrada correta")
        void callNext_publicaEventoPatientCalled() {
            enqueue("11111111111", "Ana", LocalDate.of(1980, 3, 1), EPriorityGroup.GERAL, ERiskColor.VERMELHO);

            QueueEntry chamado = callNextPatient.run();

            verify(eventPublisher, times(1)).publishPatientCalled(chamado);
        }

        @Test
        @DisplayName("Ao registrar 3 pacientes, publica exatamente 3 eventos patient.registered")
        void tresRegistros_publicaTresEventos() {
            enqueue("11111111111", "Ana",     LocalDate.of(1980, 1, 1), EPriorityGroup.GERAL, ERiskColor.VERMELHO);
            enqueue("22222222222", "Beatriz", LocalDate.of(1990, 6, 1), EPriorityGroup.GERAL, ERiskColor.AMARELO);
            enqueue("33333333333", "Carlos",  LocalDate.of(1985, 3, 1), EPriorityGroup.GERAL, ERiskColor.VERDE);

            verify(eventPublisher, times(3)).publishPatientRegistered(any(QueueEntry.class));
        }
    }
}
