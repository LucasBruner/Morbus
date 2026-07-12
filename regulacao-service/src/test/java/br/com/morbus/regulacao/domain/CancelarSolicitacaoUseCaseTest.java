package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoPendenteException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.domain.usecase.solicitacao.CancelarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelarSolicitacaoUseCase")
class CancelarSolicitacaoUseCaseTest {

    @Mock
    private ISolicitacaoRepository repository;

    private CancelarSolicitacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CancelarSolicitacaoUseCase(repository);
    }

    private Solicitacao buildSolicitacao(EStatusSolicitacao status) {
        return new Solicitacao(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                status,
                ERiscoSolicitado.AZUL,
                "I10",
                "justificativa clinica",
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
    @DisplayName("quando solicitacao esta AGUARDANDO")
    class QuandoAguardando {

        @Test
        @DisplayName("deve transicionar o status para CANCELADA")
        void deveTransicionarParaCancelada() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGUARDANDO);
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);
            when(repository.save(any())).thenReturn(solicitacao);

            useCase.execute(solicitacao.getId());

            assertThat(solicitacao.getStatus()).isEqualTo(EStatusSolicitacao.CANCELADA);
        }

        @Test
        @DisplayName("deve persistir a solicitacao cancelada")
        void devePersistir() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGUARDANDO);
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);
            when(repository.save(any())).thenReturn(solicitacao);

            useCase.execute(solicitacao.getId());

            verify(repository).save(solicitacao);
        }
    }

    @Nested
    @DisplayName("quando a solicitacao nao existe")
    class QuandoNaoExiste {

        @Test
        @DisplayName("deve propagar SolicitacaoNaoEncontradaException")
        void devePropagarSolicitacaoNaoEncontradaException() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenThrow(new SolicitacaoNaoEncontradaException("nao encontrada"));

            assertThatThrownBy(() -> useCase.execute(id))
                    .isInstanceOf(SolicitacaoNaoEncontradaException.class);
        }

        @Test
        @DisplayName("nao deve salvar quando a solicitacao nao existe")
        void naoDeveSalvar() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenThrow(new SolicitacaoNaoEncontradaException("nao encontrada"));

            assertThatThrownBy(() -> useCase.execute(id))
                    .isInstanceOf(SolicitacaoNaoEncontradaException.class);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("quando status nao permite cancelamento")
    class QuandoStatusNaoPermitido {

        @ParameterizedTest(name = "status {0} deve lancar SolicitacaoNaoPendenteException")
        @EnumSource(value = EStatusSolicitacao.class,
                names = {"APROVADA", "NEGADA", "CANCELADA", "DEVOLVIDA", "PENDENTE", "AGENDADA", "ATENDIDA", "FALTOU"})
        @DisplayName("deve lancar SolicitacaoNaoPendenteException para status invalidos")
        void deveLancarSolicitacaoNaoPendenteException(EStatusSolicitacao status) {
            Solicitacao solicitacao = buildSolicitacao(status);
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);

            assertThatThrownBy(() -> useCase.execute(solicitacao.getId()))
                    .isInstanceOf(SolicitacaoNaoPendenteException.class);
        }

        @Test
        @DisplayName("nao deve salvar quando status nao permite cancelamento")
        void naoDeveSalvar() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.APROVADA);
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);

            assertThatThrownBy(() -> useCase.execute(solicitacao.getId()))
                    .isInstanceOf(SolicitacaoNaoPendenteException.class);

            verify(repository, never()).save(any());
        }
    }
}
