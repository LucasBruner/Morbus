package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.model.Quota;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Quota")
class QuotaTest {

    @Nested
    @DisplayName("construtor completo")
    class ConstrutorCompleto {

        @Test
        @DisplayName("deve manter todos os campos informados")
        void deveManterCampos() {
            UUID id = UUID.randomUUID();
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);

            Quota quota = new Quota(id, unitId, procedureId, 10, 4, periodStart);

            assertThat(quota.getId()).isEqualTo(id);
            assertThat(quota.getUnitId()).isEqualTo(unitId);
            assertThat(quota.getProcedureId()).isEqualTo(procedureId);
            assertThat(quota.getMaxPerPeriod()).isEqualTo(10);
            assertThat(quota.getCurrentCount()).isEqualTo(4);
            assertThat(quota.getPeriodStart()).isEqualTo(periodStart);
        }
    }

    @Nested
    @DisplayName("bloqueada")
    class Bloqueada {

        @Test
        @DisplayName("deve criar cota com maxPerPeriod e currentCount iguais a zero")
        void deveCriarComLimiteZero() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);

            Quota quota = Quota.bloqueada(unitId, procedureId, periodStart);

            assertThat(quota.getMaxPerPeriod()).isZero();
            assertThat(quota.getCurrentCount()).isZero();
        }

        @Test
        @DisplayName("deve manter unitId, procedureId e periodStart informados")
        void deveManterIdentificadores() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);

            Quota quota = Quota.bloqueada(unitId, procedureId, periodStart);

            assertThat(quota.getUnitId()).isEqualTo(unitId);
            assertThat(quota.getProcedureId()).isEqualTo(procedureId);
            assertThat(quota.getPeriodStart()).isEqualTo(periodStart);
        }

        @Test
        @DisplayName("deve gerar um id nao nulo")
        void deveGerarIdNaoNulo() {
            Quota quota = Quota.bloqueada(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now());

            assertThat(quota.getId()).isNotNull();
        }

        @Test
        @DisplayName("duas cotas bloqueadas devem ter ids diferentes")
        void deveGerarIdsDiferentes() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.now();

            Quota primeira = Quota.bloqueada(unitId, procedureId, periodStart);
            Quota segunda = Quota.bloqueada(unitId, procedureId, periodStart);

            assertThat(primeira.getId()).isNotEqualTo(segunda.getId());
        }
    }
}