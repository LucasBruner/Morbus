package br.com.morbus.regulacao.adapters.out.jpa;

import br.com.morbus.regulacao.adapters.out.jpa.solicitacao.SolicitacaoEntity;
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
                "I10",
                "Hipertensao grave",
                "Dr. Silva",
                "CRM/SP 12345",
                EDestino.FILA_REGULADA,
                null,
                UUID.randomUUID(),
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                null
        );
    }

    @Nested
    @DisplayName("fromDomain")
    class FromDomain {

        @Test
        @DisplayName("deve mapear todos os campos do dominio para a entidade")
        void deveMapeiarTodosOsCampos() {
            Solicitacao s = buildDomain();
            SolicitacaoEntity entity = SolicitacaoEntity.fromDomain(s);

            assertThat(entity.getId()).isEqualTo(s.getId());
            assertThat(entity.getPatientId()).isEqualTo(s.getPatientId());
            assertThat(entity.getProcedureId()).isEqualTo(s.getProcedureId());
            assertThat(entity.getUnidadeSolicitanteId()).isEqualTo(s.getUnidadeSolicitanteId());
            assertThat(entity.getUnidadeExecutanteId()).isEqualTo(s.getUnidadeExecutanteId());
            assertThat(entity.getStatus()).isEqualTo(s.getStatus());
            assertThat(entity.getRiskColor()).isEqualTo(s.getRiskColor());
            assertThat(entity.getCid()).isEqualTo(s.getCid());
            assertThat(entity.getJustificativaClinica()).isEqualTo(s.getJustificativaClinica());
            assertThat(entity.getProfissionalSolicitante()).isEqualTo(s.getProfissionalSolicitante());
            assertThat(entity.getCrmProfissional()).isEqualTo(s.getCrmProfissional());
            assertThat(entity.getDestino()).isEqualTo(s.getDestino());
            assertThat(entity.getJustificativaNegacao()).isEqualTo(s.getJustificativaNegacao());
            assertThat(entity.getSolicitadoPor()).isEqualTo(s.getSolicitadoPor());
            assertThat(entity.getCreatedAt()).isEqualTo(s.getCreatedAt());
            assertThat(entity.getUpdatedAt()).isEqualTo(s.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("deve mapear todos os campos da entidade para o dominio")
        void deveMapeiarTodosOsCampos() {
            Solicitacao original = buildDomain();
            SolicitacaoEntity entity = SolicitacaoEntity.fromDomain(original);

            Solicitacao reconvertido = entity.toDomain();

            assertThat(reconvertido.getId()).isEqualTo(original.getId());
            assertThat(reconvertido.getPatientId()).isEqualTo(original.getPatientId());
            assertThat(reconvertido.getProcedureId()).isEqualTo(original.getProcedureId());
            assertThat(reconvertido.getUnidadeSolicitanteId()).isEqualTo(original.getUnidadeSolicitanteId());
            assertThat(reconvertido.getUnidadeExecutanteId()).isEqualTo(original.getUnidadeExecutanteId());
            assertThat(reconvertido.getStatus()).isEqualTo(original.getStatus());
            assertThat(reconvertido.getRiskColor()).isEqualTo(original.getRiskColor());
            assertThat(reconvertido.getCid()).isEqualTo(original.getCid());
            assertThat(reconvertido.getDestino()).isEqualTo(original.getDestino());
            assertThat(reconvertido.getJustificativaNegacao()).isEqualTo(original.getJustificativaNegacao());
            assertThat(reconvertido.getSolicitadoPor()).isEqualTo(original.getSolicitadoPor());
            assertThat(reconvertido.getCreatedAt()).isEqualTo(original.getCreatedAt());
            assertThat(reconvertido.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
        }

        @Test
        @DisplayName("roundtrip fromDomain -> toDomain deve preservar todos os dados")
        void roundtripDevePreservarDados() {
            Solicitacao original = buildDomain();
            Solicitacao roundtrip = SolicitacaoEntity.fromDomain(original).toDomain();

            assertThat(roundtrip.getId()).isEqualTo(original.getId());
            assertThat(roundtrip.getStatus()).isEqualTo(original.getStatus());
            assertThat(roundtrip.getCid()).isEqualTo(original.getCid());
            assertThat(roundtrip.getDestino()).isEqualTo(original.getDestino());
        }
    }
}
