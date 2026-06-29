package br.com.morbus.regulacao.adapters.out.jpa;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SolicitacaoEntity")
class SolicitacaoEntityTest {

    private Solicitacao buildDomain() {
        return new Solicitacao(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                EStatusSolicitacao.APROVADA,
                ERiscoSolicitado.AMARELO,
                "obs",
                "parecer do regulador",
                UUID.randomUUID(),
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1)
        );
    }

    // ── fromDomain ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fromDomain")
    class FromDomain {

        @Test
        @DisplayName("deve mapear todos os campos do domínio para a entidade")
        void deveMapeiarTodosOsCampos() {
            Solicitacao s = buildDomain();
            SolicitacaoEntity entity = SolicitacaoEntity.fromDomain(s);

            assertThat(entity.getId()).isEqualTo(s.getId());
            assertThat(entity.getPacienteId()).isEqualTo(s.getPacienteId());
            assertThat(entity.getProcedureId()).isEqualTo(s.getProcedureId());
            assertThat(entity.getUnidadeSolicitanteId()).isEqualTo(s.getUnidadeSolicitanteId());
            assertThat(entity.getUnidadeExecutanteId()).isEqualTo(s.getUnidadeExecutanteId());
            assertThat(entity.getStatus()).isEqualTo(s.getStatus());
            assertThat(entity.getRiscoSolicitado()).isEqualTo(s.getRiscoSolicitado());
            assertThat(entity.getObservacoes()).isEqualTo(s.getObservacoes());
            assertThat(entity.getJustificativaNegacao()).isEqualTo(s.getJustificativaNegacao());
            assertThat(entity.getSolicitadoPor()).isEqualTo(s.getSolicitadoPor());
            assertThat(entity.getCreatedAt()).isEqualTo(s.getCreatedAt());
            assertThat(entity.getUpdatedAt()).isEqualTo(s.getUpdatedAt());
        }
    }

    // ── toDomain ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("deve mapear todos os campos da entidade para o domínio")
        void deveMapeiarTodosOsCampos() {
            Solicitacao original = buildDomain();
            SolicitacaoEntity entity = SolicitacaoEntity.fromDomain(original);

            Solicitacao reconvertido = entity.toDomain();

            assertThat(reconvertido.getId()).isEqualTo(original.getId());
            assertThat(reconvertido.getPacienteId()).isEqualTo(original.getPacienteId());
            assertThat(reconvertido.getProcedureId()).isEqualTo(original.getProcedureId());
            assertThat(reconvertido.getUnidadeSolicitanteId()).isEqualTo(original.getUnidadeSolicitanteId());
            assertThat(reconvertido.getUnidadeExecutanteId()).isEqualTo(original.getUnidadeExecutanteId());
            assertThat(reconvertido.getStatus()).isEqualTo(original.getStatus());
            assertThat(reconvertido.getRiscoSolicitado()).isEqualTo(original.getRiscoSolicitado());
            assertThat(reconvertido.getObservacoes()).isEqualTo(original.getObservacoes());
            assertThat(reconvertido.getJustificativaNegacao()).isEqualTo(original.getJustificativaNegacao());
            assertThat(reconvertido.getSolicitadoPor()).isEqualTo(original.getSolicitadoPor());
            assertThat(reconvertido.getCreatedAt()).isEqualTo(original.getCreatedAt());
            assertThat(reconvertido.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
        }

        @Test
        @DisplayName("roundtrip fromDomain → toDomain deve preservar todos os dados")
        void roundtripDevePreservarDados() {
            Solicitacao original = buildDomain();
            Solicitacao roundtrip = SolicitacaoEntity.fromDomain(original).toDomain();

            assertThat(roundtrip.getId()).isEqualTo(original.getId());
            assertThat(roundtrip.getStatus()).isEqualTo(original.getStatus());
            assertThat(roundtrip.getJustificativaNegacao()).isEqualTo(original.getJustificativaNegacao());
        }
    }
}
