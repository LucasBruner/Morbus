package br.com.morbus.regulacao.adapters.out.jpa;

import br.com.morbus.regulacao.domain.dto.ListarSolicitacoesQuery;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SolicitacaoJpaAdapter")
class SolicitacaoJpaAdapterTest {

    @Mock
    private ISolicitacaoJpaRepository jpaRepository;

    private SolicitacaoJpaAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SolicitacaoJpaAdapter(jpaRepository);
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private SolicitacaoEntity buildEntity() {
        UUID id = UUID.randomUUID();
        SolicitacaoEntity e = new SolicitacaoEntity();
        e.setId(id);
        e.setPacienteId(UUID.randomUUID());
        e.setProcedureId(UUID.randomUUID());
        e.setUnidadeSolicitanteId(UUID.randomUUID());
        e.setStatus(EStatusSolicitacao.PENDENTE);
        e.setRiscoSolicitado(ERiscoSolicitado.AMARELO);
        e.setSolicitadoPor(UUID.randomUUID());
        e.setCreatedAt(LocalDateTime.now().minusHours(1));
        e.setUpdatedAt(LocalDateTime.now().minusHours(1));
        return e;
    }

    private Solicitacao buildDomain() {
        return buildEntity().toDomain();
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("deve retornar domínio quando entidade existe")
        void deveRetornarDominio() {
            SolicitacaoEntity entity = buildEntity();
            when(jpaRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

            Solicitacao result = adapter.findById(entity.getId());

            assertThat(result.getId()).isEqualTo(entity.getId());
            assertThat(result.getStatus()).isEqualTo(entity.getStatus());
        }

        @Test
        @DisplayName("deve lançar SolicitacaoNaoEncontradaException quando não existe")
        void deveLancarExcecaoQuandoNaoExiste() {
            UUID id = UUID.randomUUID();
            when(jpaRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.findById(id))
                    .isInstanceOf(SolicitacaoNaoEncontradaException.class);
        }
    }

    // ── existsAtiva ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("existsAtiva")
    class ExistsAtiva {

        @Test
        @DisplayName("deve verificar com statuses PENDENTE e APROVADA")
        void deveVerificarStatusesCorretos() {
            UUID pacienteId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            when(jpaRepository.existsByPacienteIdAndProcedureIdAndStatusIn(
                    any(), any(), any())).thenReturn(false);

            adapter.existsAtiva(pacienteId, procedureId);

            ArgumentCaptor<List<EStatusSolicitacao>> captor = ArgumentCaptor.forClass(List.class);
            verify(jpaRepository).existsByPacienteIdAndProcedureIdAndStatusIn(
                    any(), any(), captor.capture());

            assertThat(captor.getValue()).containsExactlyInAnyOrder(
                    EStatusSolicitacao.PENDENTE, EStatusSolicitacao.APROVADA);
        }

        @Test
        @DisplayName("deve retornar true quando repositório retorna true")
        void deveRetornarTrue() {
            UUID pacienteId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            when(jpaRepository.existsByPacienteIdAndProcedureIdAndStatusIn(any(), any(), any()))
                    .thenReturn(true);

            assertThat(adapter.existsAtiva(pacienteId, procedureId)).isTrue();
        }
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("deve persistir e retornar a entidade convertida para domínio")
        void devePersistirERetornar() {
            SolicitacaoEntity entity = buildEntity();
            Solicitacao domain = entity.toDomain();
            when(jpaRepository.save(any())).thenReturn(entity);

            Solicitacao result = adapter.save(domain);

            assertThat(result.getId()).isEqualTo(domain.getId());
            verify(jpaRepository).save(any(SolicitacaoEntity.class));
        }
    }

    // ── listar ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("deve chamar findAll com paginação e ordenação correta")
        void deveUsarPaginacaoCorreta() {
            ListarSolicitacoesQuery query = new ListarSolicitacoesQuery(null, null, null, 2, 10);
            when(jpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            adapter.listar(query);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(jpaRepository).findAll(any(Specification.class), pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(2);
            assertThat(pageable.getPageSize()).isEqualTo(10);
            assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        @Test
        @DisplayName("deve mapear entidades para domínio no resultado")
        void deveMapeiarResultado() {
            SolicitacaoEntity entity = buildEntity();
            ListarSolicitacoesQuery query = new ListarSolicitacoesQuery(null, null, null, 0, 20);
            when(jpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entity)));

            Page<Solicitacao> result = adapter.listar(query);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getId()).isEqualTo(entity.getId());
        }
    }
}
