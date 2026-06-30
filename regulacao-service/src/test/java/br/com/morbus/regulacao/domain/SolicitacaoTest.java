package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.enums.EDestino;
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
                "I10",
                "Paciente com hipertensao grave",
                "Dr. Silva",
                "CRM/SP 12345",
                EDestino.FILA_REGULADA,
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
                "I10",
                "justificativa",
                "Dr. Silva",
                null,
                EDestino.FILA_REGULADA,
                null,
                UUID.randomUUID(),
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1)
        );
    }

    @Nested
    @DisplayName("ao ser criada")
    class AoCriar {

        @Test
        @DisplayName("deve ter status AGUARDANDO")
        void deveTeStatusAguardando() {
            assertThat(buildNova().getStatus()).isEqualTo(EStatusSolicitacao.AGUARDANDO);
        }

        @Test
        @DisplayName("deve ter riskColor AZUL por padrao")
        void deveTeRiskColorAzul() {
            assertThat(buildNova().getRiskColor()).isEqualTo(ERiscoSolicitado.AZUL);
        }

        @Test
        @DisplayName("deve gerar um id nao nulo")
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
            UUID patientId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            UUID unidadeId = UUID.randomUUID();
            UUID solicitadoPor = UUID.randomUUID();

            Solicitacao s = new Solicitacao(patientId, procedureId, unidadeId,
                    "J45", "Asma persistente", "Dra. Costa", null,
                    EDestino.FILA_ESPERA, solicitadoPor);

            assertThat(s.getPatientId()).isEqualTo(patientId);
            assertThat(s.getProcedureId()).isEqualTo(procedureId);
            assertThat(s.getUnidadeSolicitanteId()).isEqualTo(unidadeId);
            assertThat(s.getCid()).isEqualTo("J45");
            assertThat(s.getJustificativaClinica()).isEqualTo("Asma persistente");
            assertThat(s.getProfissionalSolicitante()).isEqualTo("Dra. Costa");
            assertThat(s.getDestino()).isEqualTo(EDestino.FILA_ESPERA);
            assertThat(s.getSolicitadoPor()).isEqualTo(solicitadoPor);
        }
    }

    @Nested
    @DisplayName("ao cancelar")
    class AoCancelar {

        @Test
        @DisplayName("deve transicionar status para CANCELADA")
        void deveTransicionarParaCancelada() {
            Solicitacao s = buildExistente(EStatusSolicitacao.AGUARDANDO);
            s.cancelar();
            assertThat(s.getStatus()).isEqualTo(EStatusSolicitacao.CANCELADA);
        }
    }

    @Nested
    @DisplayName("ao aprovar")
    class AoAprovar {

        @Test
        @DisplayName("deve transicionar para APROVADA e definir riskColor e unidadeExecutanteId")
        void deveAprovar() {
            Solicitacao s = buildExistente(EStatusSolicitacao.AGUARDANDO);
            UUID unidadeExecutanteId = UUID.randomUUID();

            s.aprovar(ERiscoSolicitado.VERMELHO, unidadeExecutanteId);

            assertThat(s.getStatus()).isEqualTo(EStatusSolicitacao.APROVADA);
            assertThat(s.getRiskColor()).isEqualTo(ERiscoSolicitado.VERMELHO);
            assertThat(s.getUnidadeExecutanteId()).isEqualTo(unidadeExecutanteId);
        }
    }

    @Nested
    @DisplayName("ao aprovar para fila de espera")
    class AoAprovarParaFilaEspera {

        @Test
        @DisplayName("deve transicionar para APROVADA e sobrescrever destino para FILA_ESPERA")
        void deveAprovarParaFilaEspera() {
            Solicitacao s = buildExistente(EStatusSolicitacao.AGUARDANDO);

            s.aprovarParaFilaEspera(ERiscoSolicitado.AMARELO, UUID.randomUUID());

            assertThat(s.getStatus()).isEqualTo(EStatusSolicitacao.APROVADA);
            assertThat(s.getDestino()).isEqualTo(EDestino.FILA_ESPERA);
        }
    }

    @Nested
    @DisplayName("ao negar")
    class AoNegar {

        @Test
        @DisplayName("deve transicionar para NEGADA e definir a justificativa")
        void deveNegar() {
            Solicitacao s = buildExistente(EStatusSolicitacao.AGUARDANDO);

            s.negar("sem indicacao clinica");

            assertThat(s.getStatus()).isEqualTo(EStatusSolicitacao.NEGADA);
            assertThat(s.getJustificativaNegacao()).isEqualTo("sem indicacao clinica");
        }
    }

    @Nested
    @DisplayName("ao devolver")
    class AoDevolver {

        @Test
        @DisplayName("deve transicionar para DEVOLVIDA e definir a justificativa")
        void deveDevolver() {
            Solicitacao s = buildExistente(EStatusSolicitacao.AGUARDANDO);

            s.devolver("documentacao incompleta");

            assertThat(s.getStatus()).isEqualTo(EStatusSolicitacao.DEVOLVIDA);
            assertThat(s.getJustificativaNegacao()).isEqualTo("documentacao incompleta");
        }
    }

    @Nested
    @DisplayName("ao marcar como pendente")
    class AoMarcarPendente {

        @Test
        @DisplayName("deve transicionar para PENDENTE")
        void deveMarcarPendente() {
            Solicitacao s = buildExistente(EStatusSolicitacao.AGUARDANDO);

            s.marcarPendente();

            assertThat(s.getStatus()).isEqualTo(EStatusSolicitacao.PENDENTE);
        }
    }

    @Nested
    @DisplayName("ao reclassificar risco")
    class AoReclassificarRisco {

        @Test
        @DisplayName("deve atualizar o riskColor")
        void deveAtualizarRiskColor() {
            Solicitacao s = buildExistente(EStatusSolicitacao.APROVADA);

            s.reclassificarRisco(ERiscoSolicitado.VERMELHO);

            assertThat(s.getRiskColor()).isEqualTo(ERiscoSolicitado.VERMELHO);
        }
    }
}
