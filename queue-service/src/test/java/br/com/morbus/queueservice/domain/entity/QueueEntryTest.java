package br.com.morbus.queueservice.domain.entity;

import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QueueEntry")
class QueueEntryTest {

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Patient buildPatient(EPriorityGroup grupo) {
        return Patient.builder()
                .id(UUID.randomUUID())
                .nome("João")
                .sobrenome("Silva")
                .cpf("123.456.789-00")
                .dataNascimento(LocalDate.of(1990, 1, 1))
                .grupoLegal(grupo)
                .build();
    }

    private QueueEntry buildEntry(ERiskColor color, EQueueStatus status, Patient patient) {
        return QueueEntry.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .riskColor(color)
                .queueStatus(status)
                .registeredAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    private QueueEntry buildEntry(ERiskColor color, EQueueStatus status) {
        return buildEntry(color, status, buildPatient(EPriorityGroup.GERAL));
    }

    // ── call() ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("call()")
    class Call {

        @Test
        @DisplayName("deve alterar o status para AGENDADO")
        void deveAlterarStatusParaAgendado() {
            QueueEntry entry = buildEntry(ERiskColor.AZUL, EQueueStatus.AGUARDANDO);

            entry.call();

            assertThat(entry.getQueueStatus()).isEqualTo(EQueueStatus.AGENDADO);
        }

        @Test
        @DisplayName("deve preencher updatedAt com o momento atual")
        void devePreencherUpdatedAt() {
            QueueEntry entry = buildEntry(ERiskColor.AZUL, EQueueStatus.AGUARDANDO);

            LocalDateTime antes = LocalDateTime.now();
            entry.call();
            LocalDateTime depois = LocalDateTime.now();

            assertThat(entry.getUpdatedAt())
                    .isNotNull()
                    .isAfterOrEqualTo(antes)
                    .isBeforeOrEqualTo(depois);
        }

        @Test
        @DisplayName("não deve alterar a riskColor")
        void naoDeveAlterarRiskColor() {
            QueueEntry entry = buildEntry(ERiskColor.VERMELHO, EQueueStatus.AGUARDANDO);

            entry.call();

            assertThat(entry.getRiskColor()).isEqualTo(ERiskColor.VERMELHO);
        }
    }

    // ── cancelQueue() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelQueue()")
    class CancelQueue {

        @Test
        @DisplayName("deve alterar o status para CANCELADO")
        void deveAlterarStatusParaCancelado() {
            QueueEntry entry = buildEntry(ERiskColor.AZUL, EQueueStatus.AGUARDANDO);

            entry.cancelQueue();

            assertThat(entry.getQueueStatus()).isEqualTo(EQueueStatus.CANCELADO);
        }

        @Test
        @DisplayName("deve preencher updatedAt com o momento atual")
        void devePreencherUpdatedAt() {
            QueueEntry entry = buildEntry(ERiskColor.VERDE, EQueueStatus.AGENDADO);

            LocalDateTime antes = LocalDateTime.now();
            entry.cancelQueue();
            LocalDateTime depois = LocalDateTime.now();

            assertThat(entry.getUpdatedAt())
                    .isNotNull()
                    .isAfterOrEqualTo(antes)
                    .isBeforeOrEqualTo(depois);
        }

        @Test
        @DisplayName("não deve alterar a riskColor")
        void naoDeveAlterarRiskColor() {
            QueueEntry entry = buildEntry(ERiskColor.AMARELO, EQueueStatus.AGUARDANDO);

            entry.cancelQueue();

            assertThat(entry.getRiskColor()).isEqualTo(ERiskColor.AMARELO);
        }
    }

    // ── updateRiskColor() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateRiskColor()")
    class UpdateRiskColor {

        @Test
        @DisplayName("deve atualizar a riskColor para o novo valor")
        void deveAtualizarRiskColor() {
            QueueEntry entry = buildEntry(ERiskColor.AZUL, EQueueStatus.AGUARDANDO);

            entry.updateRiskColor(ERiskColor.VERMELHO);

            assertThat(entry.getRiskColor()).isEqualTo(ERiskColor.VERMELHO);
        }

        @Test
        @DisplayName("deve preencher updatedAt com o momento atual")
        void devePreencherUpdatedAt() {
            QueueEntry entry = buildEntry(ERiskColor.VERDE, EQueueStatus.AGUARDANDO);

            LocalDateTime antes = LocalDateTime.now();
            entry.updateRiskColor(ERiskColor.AMARELO);
            LocalDateTime depois = LocalDateTime.now();

            assertThat(entry.getUpdatedAt())
                    .isNotNull()
                    .isAfterOrEqualTo(antes)
                    .isBeforeOrEqualTo(depois);
        }

        @Test
        @DisplayName("deve recalcular o scorePosicaoCalculada após atualizar a cor")
        void deveRecalcularScore() {
            QueueEntry entry = buildEntry(ERiskColor.AZUL, EQueueStatus.AGUARDANDO,
                    buildPatient(EPriorityGroup.GERAL));
            // AZUL(4) * 10 + GERAL(6) = 46
            entry.updateRiskColor(ERiskColor.VERMELHO);
            // VERMELHO(1) * 10 + GERAL(6) = 16

            assertThat(entry.getScorePosicaoCalculada()).isEqualTo(16);
        }

        @Test
        @DisplayName("não deve alterar o status")
        void naoDeveAlterarStatus() {
            QueueEntry entry = buildEntry(ERiskColor.AZUL, EQueueStatus.AGUARDANDO);

            entry.updateRiskColor(ERiskColor.VERMELHO);

            assertThat(entry.getQueueStatus()).isEqualTo(EQueueStatus.AGUARDANDO);
        }
    }

    // ── calculatePriorityScore() ──────────────────────────────────────────────

    @Nested
    @DisplayName("calculatePriorityScore()")
    class CalculatePriorityScore {

        @Test
        @DisplayName("deve calcular score com VERMELHO e IDOSO: 1*10 + 1 = 11")
        void deveCalcularScoreVermelhoIdoso() {
            QueueEntry entry = buildEntry(ERiskColor.VERMELHO, EQueueStatus.AGUARDANDO,
                    buildPatient(EPriorityGroup.IDOSO));

            entry.calculatePriorityScore();

            assertThat(entry.getScorePosicaoCalculada()).isEqualTo(11);
        }

        @Test
        @DisplayName("deve calcular score com AZUL e GERAL: 4*10 + 6 = 46")
        void deveCalcularScoreAzulGeral() {
            QueueEntry entry = buildEntry(ERiskColor.AZUL, EQueueStatus.AGUARDANDO,
                    buildPatient(EPriorityGroup.GERAL));

            entry.calculatePriorityScore();

            assertThat(entry.getScorePosicaoCalculada()).isEqualTo(46);
        }

        @Test
        @DisplayName("deve calcular score com AMARELO e GESTANTE: 2*10 + 2 = 22")
        void deveCalcularScoreAmareloGestante() {
            QueueEntry entry = buildEntry(ERiskColor.AMARELO, EQueueStatus.AGUARDANDO,
                    buildPatient(EPriorityGroup.GESTANTE));

            entry.calculatePriorityScore();

            assertThat(entry.getScorePosicaoCalculada()).isEqualTo(22);
        }

        @Test
        @DisplayName("deve usar colorScore=4 (AZUL) quando riskColor for nulo")
        void deveUsarFallbackDeCorQuandoNulo() {
            QueueEntry entry = QueueEntry.builder()
                    .id(UUID.randomUUID())
                    .patient(buildPatient(EPriorityGroup.GERAL))
                    .riskColor(null)
                    .queueStatus(EQueueStatus.AGUARDANDO)
                    .registeredAt(LocalDateTime.now())
                    .build();

            entry.calculatePriorityScore();

            // null → colorScore=4, GERAL → groupScore=6 → 4*10+6 = 46
            assertThat(entry.getScorePosicaoCalculada()).isEqualTo(46);
        }

        @Test
        @DisplayName("deve usar groupScore=6 (GERAL) quando patient for nulo")
        void deveUsarFallbackDeGrupoQuandoPatientNulo() {
            QueueEntry entry = QueueEntry.builder()
                    .id(UUID.randomUUID())
                    .patient(null)
                    .riskColor(ERiskColor.VERMELHO)
                    .queueStatus(EQueueStatus.AGUARDANDO)
                    .registeredAt(LocalDateTime.now())
                    .build();

            entry.calculatePriorityScore();

            // VERMELHO → colorScore=1, null → groupScore=6 → 1*10+6 = 16
            assertThat(entry.getScorePosicaoCalculada()).isEqualTo(16);
        }

        @Test
        @DisplayName("deve usar groupScore=6 (GERAL) quando grupoLegal do patient for nulo")
        void deveUsarFallbackDeGrupoQuandoGrupoLegalNulo() {
            Patient patientSemGrupo = Patient.builder()
                    .id(UUID.randomUUID())
                    .nome("Sem Grupo")
                    .cpf("000.000.000-00")
                    .dataNascimento(LocalDate.of(1990, 1, 1))
                    .grupoLegal(null)
                    .build();

            QueueEntry entry = buildEntry(ERiskColor.VERDE, EQueueStatus.AGUARDANDO, patientSemGrupo);

            entry.calculatePriorityScore();

            // VERDE → colorScore=3, null → groupScore=6 → 3*10+6 = 36
            assertThat(entry.getScorePosicaoCalculada()).isEqualTo(36);
        }

        @Test
        @DisplayName("score menor indica maior prioridade (VERMELHO+IDOSO < AZUL+GERAL)")
        void scoreMenorIndicaMaiorPrioridade() {
            QueueEntry alta = buildEntry(ERiskColor.VERMELHO, EQueueStatus.AGUARDANDO,
                    buildPatient(EPriorityGroup.IDOSO));
            QueueEntry baixa = buildEntry(ERiskColor.AZUL, EQueueStatus.AGUARDANDO,
                    buildPatient(EPriorityGroup.GERAL));

            alta.calculatePriorityScore();
            baixa.calculatePriorityScore();

            assertThat(alta.getScorePosicaoCalculada())
                    .isLessThan(baixa.getScorePosicaoCalculada());
        }
    }
}
