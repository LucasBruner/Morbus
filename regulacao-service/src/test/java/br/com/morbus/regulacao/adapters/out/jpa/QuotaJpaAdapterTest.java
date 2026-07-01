package br.com.morbus.regulacao.adapters.out.jpa;

import br.com.morbus.regulacao.adapters.out.jpa.quota.IQuotaJpaRepository;
import br.com.morbus.regulacao.adapters.out.jpa.quota.QuotaEntity;
import br.com.morbus.regulacao.adapters.out.jpa.quota.QuotaJpaAdapter;
import br.com.morbus.regulacao.domain.model.Quota;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuotaJpaAdapter")
class QuotaJpaAdapterTest {

    @Mock
    private IQuotaJpaRepository jpaRepository;

    private QuotaJpaAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new QuotaJpaAdapter(jpaRepository);
    }

    private QuotaEntity buildEntity(UUID unitId, UUID procedureId, LocalDate periodStart, int maxPerPeriod, int currentCount) {
        return new QuotaEntity(UUID.randomUUID(), unitId, procedureId, maxPerPeriod, currentCount, periodStart);
    }

    @Nested
    @DisplayName("findOrCreate")
    class FindOrCreate {

        @Test
        @DisplayName("deve retornar a cota existente sem criar nova quando ja existe registro")
        void deveRetornarCotaExistente() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);
            QuotaEntity existente = buildEntity(unitId, procedureId, periodStart, 5, 2);
            when(jpaRepository.findByUnitIdAndProcedureIdAndPeriodStart(unitId, procedureId, periodStart))
                    .thenReturn(Optional.of(existente));

            Quota result = adapter.findOrCreate(unitId, procedureId, periodStart);

            assertThat(result.getId()).isEqualTo(existente.getId());
            assertThat(result.getMaxPerPeriod()).isEqualTo(5);
            assertThat(result.getCurrentCount()).isEqualTo(2);
            verify(jpaRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve criar cota bloqueada (max=0) quando nao existe registro")
        void deveCriarCotaBloqueadaQuandoNaoExiste() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);
            when(jpaRepository.findByUnitIdAndProcedureIdAndPeriodStart(unitId, procedureId, periodStart))
                    .thenReturn(Optional.empty());
            when(jpaRepository.save(any(QuotaEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Quota result = adapter.findOrCreate(unitId, procedureId, periodStart);

            assertThat(result.getMaxPerPeriod()).isZero();
            assertThat(result.getCurrentCount()).isZero();
            assertThat(result.getUnitId()).isEqualTo(unitId);
            assertThat(result.getProcedureId()).isEqualTo(procedureId);
            assertThat(result.getPeriodStart()).isEqualTo(periodStart);
        }

        @Test
        @DisplayName("deve persistir a cota bloqueada quando nao existe registro")
        void devePersistirCotaBloqueada() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);
            when(jpaRepository.findByUnitIdAndProcedureIdAndPeriodStart(unitId, procedureId, periodStart))
                    .thenReturn(Optional.empty());
            when(jpaRepository.save(any(QuotaEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            adapter.findOrCreate(unitId, procedureId, periodStart);

            ArgumentCaptor<QuotaEntity> captor = ArgumentCaptor.forClass(QuotaEntity.class);
            verify(jpaRepository).save(captor.capture());
            assertThat(captor.getValue().getMaxPerPeriod()).isZero();
            assertThat(captor.getValue().getUnitId()).isEqualTo(unitId);
            assertThat(captor.getValue().getProcedureId()).isEqualTo(procedureId);
            assertThat(captor.getValue().getPeriodStart()).isEqualTo(periodStart);
        }
    }

    @Nested
    @DisplayName("incrementarSeDisponivel")
    class IncrementarSeDisponivel {

        @Test
        @DisplayName("deve retornar true quando o repositorio atualiza uma linha")
        void deveRetornarTrueQuandoAtualizaUmaLinha() {
            UUID quotaId = UUID.randomUUID();
            when(jpaRepository.incrementarSeDisponivel(quotaId)).thenReturn(1);

            boolean result = adapter.incrementarSeDisponivel(quotaId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("deve retornar false quando o repositorio nao atualiza nenhuma linha")
        void deveRetornarFalseQuandoNaoAtualizaLinha() {
            UUID quotaId = UUID.randomUUID();
            when(jpaRepository.incrementarSeDisponivel(quotaId)).thenReturn(0);

            boolean result = adapter.incrementarSeDisponivel(quotaId);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("deve delegar o id correto para o repositorio jpa")
        void deveDelegarIdCorreto() {
            UUID quotaId = UUID.randomUUID();
            when(jpaRepository.incrementarSeDisponivel(quotaId)).thenReturn(1);

            adapter.incrementarSeDisponivel(quotaId);

            verify(jpaRepository).incrementarSeDisponivel(quotaId);
        }
    }
}