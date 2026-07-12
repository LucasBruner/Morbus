package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.dto.UsuarioContexto;
import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.domain.usecase.solicitacao.ConsultarStatusSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarStatusSolicitacaoUseCase")
class ConsultarStatusSolicitacaoUseCaseTest {

    @Mock
    private ISolicitacaoRepository repository;

    private ConsultarStatusSolicitacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarStatusSolicitacaoUseCase(repository);
    }

    private Solicitacao buildSolicitacao() {
        return new Solicitacao(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                EStatusSolicitacao.AGUARDANDO,
                ERiscoSolicitado.AZUL,
                "I10",
                "Hipertensao grave",
                "Dr. Silva",
                null,
                EDestino.FILA_REGULADA,
                null,
                UUID.randomUUID(),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusHours(1),
                null,
                null
        );
    }

    @Nested
    @DisplayName("quando role e ROLE_SOLICITANTE")
    class QuandoSolicitante {

        @Test
        @DisplayName("deve retornar a solicitacao")
        void deveRetornar() {
            Solicitacao solicitacao = buildSolicitacao();
            UsuarioContexto contexto = new UsuarioContexto("ROLE_SOLICITANTE", UUID.randomUUID());
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);

            Solicitacao result = useCase.execute(solicitacao.getId(), contexto);

            assertThat(result).isSameAs(solicitacao);
        }
    }

    @Nested
    @DisplayName("quando role e ROLE_REGULADOR")
    class QuandoRegulador {

        @Test
        @DisplayName("deve retornar a solicitacao")
        void deveRetornar() {
            Solicitacao solicitacao = buildSolicitacao();
            UsuarioContexto contexto = new UsuarioContexto("ROLE_REGULADOR", UUID.randomUUID());
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);

            Solicitacao result = useCase.execute(solicitacao.getId(), contexto);

            assertThat(result).isSameAs(solicitacao);
        }
    }

    @Nested
    @DisplayName("quando a solicitacao nao existe")
    class QuandoNaoExiste {

        @Test
        @DisplayName("deve propagar SolicitacaoNaoEncontradaException")
        void devePropagarSolicitacaoNaoEncontradaException() {
            UUID id = UUID.randomUUID();
            UsuarioContexto contexto = new UsuarioContexto("ROLE_SOLICITANTE", UUID.randomUUID());
            when(repository.findById(id)).thenThrow(new SolicitacaoNaoEncontradaException("nao encontrada"));

            assertThatThrownBy(() -> useCase.execute(id, contexto))
                    .isInstanceOf(SolicitacaoNaoEncontradaException.class);
        }
    }
}
