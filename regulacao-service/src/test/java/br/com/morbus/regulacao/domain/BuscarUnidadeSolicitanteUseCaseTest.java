package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.exception.UnidadeSolicitanteNaoEncontradaException;
import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;
import br.com.morbus.regulacao.domain.usecase.unidade.BuscarUnidadeSolicitanteUseCase;
import br.com.morbus.regulacao.ports.out.IUnidadeSolicitanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BuscarUnidadeSolicitanteUseCase")
class BuscarUnidadeSolicitanteUseCaseTest {

    @Mock
    private IUnidadeSolicitanteRepository unidadeSolicitanteRepository;

    private BuscarUnidadeSolicitanteUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BuscarUnidadeSolicitanteUseCase(unidadeSolicitanteRepository);
    }

    @Test
    @DisplayName("deve retornar a unidade quando o id existe")
    void deveRetornarUnidadeQuandoExiste() {
        UUID id = UUID.randomUUID();
        UnidadeSolicitante unidade = new UnidadeSolicitante(id, "1234567", "UBS Central", "Rua A, 1", "1111-1111");
        when(unidadeSolicitanteRepository.buscarPorId(id)).thenReturn(Optional.of(unidade));

        UnidadeSolicitante result = useCase.execute(id);

        assertThat(result).isSameAs(unidade);
    }

    @Test
    @DisplayName("deve lancar UnidadeSolicitanteNaoEncontradaException quando o id nao existe")
    void deveLancarExcecaoQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(unidadeSolicitanteRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id))
                .isInstanceOf(UnidadeSolicitanteNaoEncontradaException.class);
    }
}