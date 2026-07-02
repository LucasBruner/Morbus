package br.com.morbus.regulacao.adapters.out.jpa;

import br.com.morbus.regulacao.adapters.out.jpa.unidade.IUnidadeSolicitanteJpaRepository;
import br.com.morbus.regulacao.adapters.out.jpa.unidade.UnidadeSolicitanteEntity;
import br.com.morbus.regulacao.adapters.out.jpa.unidade.UnidadeSolicitanteJpaAdapter;
import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnidadeSolicitanteJpaAdapter")
class UnidadeSolicitanteJpaAdapterTest {

    @Mock
    private IUnidadeSolicitanteJpaRepository jpaRepository;

    private UnidadeSolicitanteJpaAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UnidadeSolicitanteJpaAdapter(jpaRepository);
    }

    @Nested
    @DisplayName("salvar")
    class Salvar {

        @Test
        @DisplayName("deve persistir e retornar a unidade convertida para dominio")
        void devePersistirERetornar() {
            UnidadeSolicitante unidade = new UnidadeSolicitante("1234567", "UBS Central", "Rua A, 1", "1111-1111");
            UnidadeSolicitanteEntity entitySalva = UnidadeSolicitanteEntity.fromDomain(unidade);
            when(jpaRepository.save(any(UnidadeSolicitanteEntity.class))).thenReturn(entitySalva);

            UnidadeSolicitante result = adapter.salvar(unidade);

            assertThat(result.getId()).isEqualTo(unidade.getId());
            assertThat(result.getCnes()).isEqualTo("1234567");
            verify(jpaRepository).save(any(UnidadeSolicitanteEntity.class));
        }
    }

    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("deve retornar a unidade convertida para dominio quando existe")
        void deveRetornarQuandoExiste() {
            UUID id = UUID.randomUUID();
            UnidadeSolicitanteEntity entity = UnidadeSolicitanteEntity.fromDomain(
                    new UnidadeSolicitante(id, "1234567", "UBS Central", "Rua A, 1", "1111-1111"));
            when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));

            Optional<UnidadeSolicitante> result = adapter.buscarPorId(id);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("deve retornar Optional vazio quando nao existe")
        void deveRetornarVazioQuandoNaoExiste() {
            UUID id = UUID.randomUUID();
            when(jpaRepository.findById(id)).thenReturn(Optional.empty());

            Optional<UnidadeSolicitante> result = adapter.buscarPorId(id);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsById")
    class ExistsById {

        @Test
        @DisplayName("deve delegar para o repositorio jpa")
        void deveDelegarParaRepositorio() {
            UUID id = UUID.randomUUID();
            when(jpaRepository.existsById(id)).thenReturn(true);

            boolean result = adapter.existsById(id);

            assertThat(result).isTrue();
            verify(jpaRepository).existsById(id);
        }
    }

    @Nested
    @DisplayName("existsByCnes")
    class ExistsByCnes {

        @Test
        @DisplayName("deve delegar para o repositorio jpa")
        void deveDelegarParaRepositorio() {
            when(jpaRepository.existsByCnes("1234567")).thenReturn(true);

            boolean result = adapter.existsByCnes("1234567");

            assertThat(result).isTrue();
            verify(jpaRepository).existsByCnes("1234567");
        }
    }
}