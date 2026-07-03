package br.com.morbus.regulacao.adapters.out.jpa;

import br.com.morbus.regulacao.adapters.out.jpa.quota.QuotaEntity;
import br.com.morbus.regulacao.domain.model.Quota;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuotaEntity")
class QuotaEntityTest {

    private Quota buildDomain() {
        return new Quota(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                10,
                3,
                LocalDate.of(2026, 7, 1)
        );
    }

    @Nested
    @DisplayName("fromDomain")
    class FromDomain {

        @Test
        @DisplayName("deve mapear todos os campos do dominio para a entidade")
        void deveMapearTodosOsCampos() {
            Quota quota = buildDomain();

            QuotaEntity entity = QuotaEntity.fromDomain(quota);

            assertThat(entity.getId()).isEqualTo(quota.getId());
            assertThat(entity.getUnitId()).isEqualTo(quota.getUnitId());
            assertThat(entity.getProcedureId()).isEqualTo(quota.getProcedureId());
            assertThat(entity.getMaxPerPeriod()).isEqualTo(quota.getMaxPerPeriod());
            assertThat(entity.getCurrentCount()).isEqualTo(quota.getCurrentCount());
            assertThat(entity.getPeriodStart()).isEqualTo(quota.getPeriodStart());
        }
    }

    @Nested
    @DisplayName("toDomain")
    class ToDomain {

        @Test
        @DisplayName("deve mapear todos os campos da entidade para o dominio")
        void deveMapearTodosOsCampos() {
            Quota original = buildDomain();
            QuotaEntity entity = QuotaEntity.fromDomain(original);

            Quota reconvertido = entity.toDomain();

            assertThat(reconvertido.getId()).isEqualTo(original.getId());
            assertThat(reconvertido.getUnitId()).isEqualTo(original.getUnitId());
            assertThat(reconvertido.getProcedureId()).isEqualTo(original.getProcedureId());
            assertThat(reconvertido.getMaxPerPeriod()).isEqualTo(original.getMaxPerPeriod());
            assertThat(reconvertido.getCurrentCount()).isEqualTo(original.getCurrentCount());
            assertThat(reconvertido.getPeriodStart()).isEqualTo(original.getPeriodStart());
        }

        @Test
        @DisplayName("roundtrip fromDomain -> toDomain deve preservar todos os dados")
        void roundtripDevePreservarDados() {
            Quota original = buildDomain();

            Quota roundtrip = QuotaEntity.fromDomain(original).toDomain();

            assertThat(roundtrip.getId()).isEqualTo(original.getId());
            assertThat(roundtrip.getUnitId()).isEqualTo(original.getUnitId());
            assertThat(roundtrip.getProcedureId()).isEqualTo(original.getProcedureId());
            assertThat(roundtrip.getMaxPerPeriod()).isEqualTo(original.getMaxPerPeriod());
            assertThat(roundtrip.getCurrentCount()).isEqualTo(original.getCurrentCount());
            assertThat(roundtrip.getPeriodStart()).isEqualTo(original.getPeriodStart());
        }
    }
}