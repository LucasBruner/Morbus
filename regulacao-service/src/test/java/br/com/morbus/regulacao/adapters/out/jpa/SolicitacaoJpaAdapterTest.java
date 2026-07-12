package br.com.morbus.regulacao.adapters.out.jpa;

import br.com.morbus.regulacao.adapters.out.jpa.solicitacao.ISolicitacaoJpaRepository;
import br.com.morbus.regulacao.adapters.out.jpa.solicitacao.SolicitacaoEntity;
import br.com.morbus.regulacao.adapters.out.jpa.solicitacao.SolicitacaoJpaAdapter;
import br.com.morbus.regulacao.domain.dto.ESortDirection;
import br.com.morbus.regulacao.domain.dto.ListarSolicitacoesQuery;
import br.com.morbus.regulacao.domain.dto.PageResult;
import br.com.morbus.regulacao.domain.enums.EDestino;
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

    private SolicitacaoEntity buildEntity() {
        UUID id = UUID.randomUUID();
        SolicitacaoEntity e = new SolicitacaoEntity();
        e.setId(id);
        e.setPatientId(UUID.randomUUID());
        e.setProcedureId(UUID.randomUUID());
        e.setUnidadeSolicitanteId(UUID.randomUUID());
        e.setStatus(EStatusSolicitacao.AGUARDANDO);
        e.setRiskColor(ERiscoSolicitado.AZUL);
        e.setCid("I10");
        e.setJustificativaClinica("Hipertensao grave");
        e.setProfissionalSolicitante("Dr. Silva");
        e.setDestino(EDestino.FILA_REGULADA);
        e.setSolicitadoPor(UUID.randomUUID());
        e.setCreatedAt(LocalDateTime.now().minusHours(1));
        e.setUpdatedAt(LocalDateTime.now().minusHours(1));
        return e;
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("deve retornar dominio quando entidade existe")
        void deveRetornarDominio() {
            SolicitacaoEntity entity = buildEntity();
            when(jpaRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

            Solicitacao result = adapter.findById(entity.getId());

            assertThat(result.getId()).isEqualTo(entity.getId());
            assertThat(result.getStatus()).isEqualTo(EStatusSolicitacao.AGUARDANDO);
        }

        @Test
        @DisplayName("deve lancar SolicitacaoNaoEncontradaException quando nao existe")
        void deveLancarExcecaoQuandoNaoExiste() {
            UUID id = UUID.randomUUID();
            when(jpaRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.findById(id))
                    .isInstanceOf(SolicitacaoNaoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("existsAtiva")
    class ExistsAtiva {

        @Test
        @DisplayName("deve verificar com statuses AGUARDANDO e APROVADA")
        void deveVerificarStatusesCorretos() {
            UUID patientId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            when(jpaRepository.existsByPatientIdAndProcedureIdAndStatusIn(
                    any(), any(), any())).thenReturn(false);

            adapter.existsAtiva(patientId, procedureId);

            ArgumentCaptor<List<EStatusSolicitacao>> captor = ArgumentCaptor.forClass(List.class);
            verify(jpaRepository).existsByPatientIdAndProcedureIdAndStatusIn(
                    any(), any(), captor.capture());

            assertThat(captor.getValue()).containsExactlyInAnyOrder(
                    EStatusSolicitacao.AGUARDANDO, EStatusSolicitacao.APROVADA);
        }

        @Test
        @DisplayName("deve retornar true quando repositorio retorna true")
        void deveRetornarTrue() {
            UUID patientId = UUID.randomUUID();
            UUID procedureId = UUID.randomUUID();
            when(jpaRepository.existsByPatientIdAndProcedureIdAndStatusIn(any(), any(), any()))
                    .thenReturn(true);

            assertThat(adapter.existsAtiva(patientId, procedureId)).isTrue();
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("deve persistir e retornar a entidade convertida para dominio")
        void devePersistirERetornar() {
            SolicitacaoEntity entity = buildEntity();
            Solicitacao domain = entity.toDomain();
            when(jpaRepository.save(any())).thenReturn(entity);

            Solicitacao result = adapter.save(domain);

            assertThat(result.getId()).isEqualTo(domain.getId());
            verify(jpaRepository).save(any(SolicitacaoEntity.class));
        }
    }

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("deve chamar findAll com paginacao e ordenacao correta")
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
        @DisplayName("deve ordenar por createdAt ASC quando informado na query")
        void deveOrdenarAscQuandoInformado() {
            ListarSolicitacoesQuery query = new ListarSolicitacoesQuery(null, null, null, 0, 20, ESortDirection.ASC);
            when(jpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            adapter.listar(query);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(jpaRepository).findAll(any(Specification.class), pageableCaptor.capture());

            assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "createdAt"));
        }

        @Test
        @DisplayName("deve mapear entidades para dominio no resultado")
        void deveMapeiarResultado() {
            SolicitacaoEntity entity = buildEntity();
            ListarSolicitacoesQuery query = new ListarSolicitacoesQuery(null, null, null, 0, 20);
            when(jpaRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entity)));

            PageResult<Solicitacao> result = adapter.listar(query);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().getId()).isEqualTo(entity.getId());
        }
    }
}
