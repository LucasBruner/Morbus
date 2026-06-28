package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoPendenteException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
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

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Solicitacao buildSolicitacao(EStatusSolicitacao status) {
        return new Solicitacao(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                status,
                ERiscoSolicitado.VERDE,
                "observação",
                null,
                UUID.randomUUID(),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusHours(1)
        );
    }

    // ── Fluxo feliz ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando solicitação está PENDENTE")
    class QuandoPendente {

        @Test
        @DisplayName("deve transicionar o status para CANCELADA")
        void deveTransicionarParaCancelada() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.PENDENTE);
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);
            when(repository.save(any())).thenReturn(solicitacao);

            useCase.execute(solicitacao.getId());

            assertThat(solicitacao.getStatus()).isEqualTo(EStatusSolicitacao.CANCELADA);
        }

        @Test
        @DisplayName("deve persistir a solicitação cancelada")
        void devePersistir() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.PENDENTE);
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);
            when(repository.save(any())).thenReturn(solicitacao);

            useCase.execute(solicitacao.getId());

            verify(repository).save(solicitacao);
        }
    }

    // ── Não encontrado ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando a solicitação não existe")
    class QuandoNaoExiste {

        @Test
        @DisplayName("deve propagar SolicitacaoNaoEncontradaException")
        void devePropagarSolicitacaoNaoEncontradaException() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenThrow(new SolicitacaoNaoEncontradaException("não encontrada"));

            assertThatThrownBy(() -> useCase.execute(id))
                    .isInstanceOf(SolicitacaoNaoEncontradaException.class);
        }

        @Test
        @DisplayName("não deve salvar quando a solicitação não existe")
        void naoDeveSalvar() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenThrow(new SolicitacaoNaoEncontradaException("não encontrada"));

            assertThatThrownBy(() -> useCase.execute(id))
                    .isInstanceOf(SolicitacaoNaoEncontradaException.class);

            verify(repository, never()).save(any());
        }
    }

    // ── Status não permite cancelamento ──────────────────────────────────────

    @Nested
    @DisplayName("quando status não permite cancelamento")
    class QuandoStatusNaoPermitido {

        @ParameterizedTest(name = "status {0} deve lançar SolicitacaoNaoPendenteException")
        @EnumSource(value = EStatusSolicitacao.class, names = {"EM_ANALISE", "APROVADA", "NEGADA", "CANCELADA", "DEVOLVIDA"})
        @DisplayName("deve lançar SolicitacaoNaoPendenteException para status inválidos")
        void deveLancarSolicitacaoNaoPendenteException(EStatusSolicitacao status) {
            Solicitacao solicitacao = buildSolicitacao(status);
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);

            assertThatThrownBy(() -> useCase.execute(solicitacao.getId()))
                    .isInstanceOf(SolicitacaoNaoPendenteException.class);
        }

        @Test
        @DisplayName("não deve salvar quando status não permite cancelamento")
        void naoDeveSalvar() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.APROVADA);
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);

            assertThatThrownBy(() -> useCase.execute(solicitacao.getId()))
                    .isInstanceOf(SolicitacaoNaoPendenteException.class);

            verify(repository, never()).save(any());
        }
    }
}