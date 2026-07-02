package br.com.morbus.regulacao.adapters.out.jpa;

import br.com.morbus.regulacao.adapters.out.jpa.quota.IQuotaJpaRepository;
import br.com.morbus.regulacao.adapters.out.jpa.quota.QuotaEntity;
import br.com.morbus.regulacao.adapters.out.jpa.quota.QuotaJpaAdapter;
import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.ports.in.dto.ConsultarCotasQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
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

            Quota result = adapter.findOrCreate(unitId, procedureId, periodStart);

            assertThat(result.getMaxPerPeriod()).isZero();
            assertThat(result.getCurrentCount()).isZero();
            assertThat(result.getUnitId()).isEqualTo(unitId);
            assertThat(result.getProcedureId()).isEqualTo(procedureId);
            assertThat(result.getPeriodStart()).isEqualTo(periodStart);
        }

        @Test
        @DisplayName("nao deve persistir a cota bloqueada quando nao existe registro")
        void naoDevePersistirCotaBloqueada() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);
            when(jpaRepository.findByUnitIdAndProcedureIdAndPeriodStart(unitId, procedureId, periodStart))
                    .thenReturn(Optional.empty());

            adapter.findOrCreate(unitId, procedureId, periodStart);

            verify(jpaRepository, never()).save(any());
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

    @Nested
    @DisplayName("buscarPorChave")
    class BuscarPorChave {

        @Test
        @DisplayName("deve retornar a cota convertida para dominio quando existe")
        void deveRetornarCotaQuandoExiste() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);
            QuotaEntity entity = buildEntity(unitId, procedureId, periodStart, 10, 3);
            when(jpaRepository.findByUnitIdAndProcedureIdAndPeriodStart(unitId, procedureId, periodStart))
                    .thenReturn(Optional.of(entity));

            Optional<Quota> result = adapter.buscarPorChave(unitId, procedureId, periodStart);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(entity.getId());
            assertThat(result.get().getMaxPerPeriod()).isEqualTo(10);
        }

        @Test
        @DisplayName("deve retornar Optional vazio quando nao existe, sem criar registro")
        void deveRetornarVazioQuandoNaoExiste() {
            UUID unitId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            LocalDate periodStart = LocalDate.of(2026, 7, 1);
            when(jpaRepository.findByUnitIdAndProcedureIdAndPeriodStart(unitId, procedureId, periodStart))
                    .thenReturn(Optional.empty());

            Optional<Quota> result = adapter.buscarPorChave(unitId, procedureId, periodStart);

            assertThat(result).isEmpty();
            verify(jpaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("salvar")
    class Salvar {

        @Test
        @DisplayName("deve persistir e retornar a cota convertida para dominio")
        void devePersistirERetornar() {
            Quota quota = new Quota(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    15, 0, LocalDate.of(2026, 7, 1));
            QuotaEntity entitySalva = QuotaEntity.fromDomain(quota);
            when(jpaRepository.save(any(QuotaEntity.class))).thenReturn(entitySalva);

            Quota result = adapter.salvar(quota);

            assertThat(result.getId()).isEqualTo(quota.getId());
            assertThat(result.getMaxPerPeriod()).isEqualTo(15);
            verify(jpaRepository).save(any(QuotaEntity.class));
        }
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("deve usar a paginacao informada na query")
        void deveUsarPaginacaoCorreta() {
            ConsultarCotasQuery query = new ConsultarCotasQuery(null, null, null, 2, 10);
            when(jpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            adapter.listar(query);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(jpaRepository).findAll(any(Specification.class), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("deve mapear entidades para dominio no resultado")
        void deveMapearResultado() {
            QuotaEntity entity = buildEntity(UUID.randomUUID(), UUID.randomUUID(),
                    LocalDate.of(2026, 7, 1), 10, 3);
            ConsultarCotasQuery query = new ConsultarCotasQuery(null, null, null, 0, 20);
            when(jpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entity)));

            Page<Quota> result = adapter.listar(query);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getId()).isEqualTo(entity.getId());
        }

        @Test
        @DisplayName("deve retornar pagina vazia quando nao ha cotas")
        void deveRetornarPaginaVazia() {
            ConsultarCotasQuery query = new ConsultarCotasQuery(
                    UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 7, 1), 0, 20);
            when(jpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            Page<Quota> result = adapter.listar(query);

            assertThat(result.getContent()).isEmpty();
        }
    }
}