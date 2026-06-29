package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Solicitacao")
class SolicitacaoTest {

    private Solicitacao buildNova() {
        return new Solicitacao(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                ERiscoSolicitado.AMARELO,
                "observação",
                UUID.randomUUID()
        );
    }

    private Solicitacao buildExistente(EStatusSolicitacao status) {
        return new Solicitacao(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                status,
                ERiscoSolicitado.VERDE,
                "obs",
                null,
                UUID.randomUUID(),
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1)
        );
    }

    // ── Construtor de criação ────────────────────────────────────────────────

    @Nested
    @DisplayName("ao ser criada")
    class AoCriar {

        @Test
        @DisplayName("deve ter status PENDENTE")
        void deveTeStatusPendente() {
            assertThat(buildNova().getStatus()).isEqualTo(EStatusSolicitacao.PENDENTE);
        }

        @Test
        @DisplayName("deve gerar um id não nulo")
        void deveGerarId() {
            assertThat(buildNova().getId()).isNotNull();
        }

        @Test
        @DisplayName("deve definir createdAt como agora")
        void deveDefinirCreatedAt() {
            LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
            Solicitacao s = buildNova();
            assertThat(s.getCreatedAt()).isAfterOrEqualTo(antes);
        }

        @Test
        @DisplayName("deve definir updatedAt como agora")
        void deveDefinirUpdatedAt() {
            LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
            Solicitacao s = buildNova();
            assertThat(s.getUpdatedAt()).isAfterOrEqualTo(antes);
        }

        @Test
        @DisplayName("deve preservar os campos informados")
        void devePreservarCampos() {
            UUID pacienteId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            UUID unidadeId = UUID.randomUUID();
            UUID solicitadoPor = UUID.randomUUID();

            Solicitacao s = new Solicitacao(pacienteId, procedureId, unidadeId,
                    ERiscoSolicitado.VERMELHO, "obs", solicitadoPor);

            assertThat(s.getPacienteId()).isEqualTo(pacienteId);
            assertThat(s.getProcedureId()).isEqualTo(procedureId);
            assertThat(s.getUnidadeSolicitanteId()).isEqualTo(unidadeId);
            assertThat(s.getRiscoSolicitado()).isEqualTo(ERiscoSolicitado.VERMELHO);
            assertThat(s.getObservacoes()).isEqualTo("obs");
            assertThat(s.getSolicitadoPor()).isEqualTo(solicitadoPor);
        }
    }

    // ── cancelar() ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ao cancelar")
    class AoCancelar {

        @Test
        @DisplayName("deve transicionar status para CANCELADA")
        void deveTransicionarParaCancelada() {
            Solicitacao s = buildExistente(EStatusSolicitacao.PENDENTE);
            s.cancelar();
            assertThat(s.getStatus()).isEqualTo(EStatusSolicitacao.CANCELADA);
        }
    }
}
