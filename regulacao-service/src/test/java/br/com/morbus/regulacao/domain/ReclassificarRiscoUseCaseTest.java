package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoPendenteException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.domain.usecase.solicitacao.ReclassificarRiscoUseCase;
import br.com.morbus.regulacao.ports.in.dto.ReclassificarRiscoCommand;
import br.com.morbus.regulacao.ports.out.IRegulacaoEventPublisher;
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
@DisplayName("ReclassificarRiscoUseCase")
class ReclassificarRiscoUseCaseTest {

    @Mock
    private ISolicitacaoRepository solicitacaoRepository;

    @Mock
    private IRegulacaoEventPublisher eventPublisher;

    private ReclassificarRiscoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReclassificarRiscoUseCase(solicitacaoRepository, eventPublisher);
    }

    private Solicitacao buildSolicitacao(EStatusSolicitacao status, EDestino destino) {
        return new Solicitacao(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                status, ERiscoSolicitado.AZUL, "I10", "justificativa clinica", "Dr. Silva", null,
                destino, null, UUID.randomUUID(),
                LocalDateTime.now().minusHours(1), LocalDateTime.now().minusHours(1), null
        );
    }

    @Nested
    @DisplayName("quando a solicitacao esta APROVADA e em FILA_REGULADA")
    class QuandoApta {

        @Test
        @DisplayName("deve atualizar o riskColor")
        void deveAtualizarRiskColor() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.APROVADA, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            when(solicitacaoRepository.save(any())).thenReturn(solicitacao);

            Solicitacao result = useCase.execute(new ReclassificarRiscoCommand(solicitacao.getId(), ERiscoSolicitado.VERMELHO));

            assertThat(result.getRiskColor()).isEqualTo(ERiscoSolicitado.VERMELHO);
        }

        @Test
        @DisplayName("deve publicar solicitation.reclassified")
        void devePublicarEvento() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.APROVADA, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            when(solicitacaoRepository.save(any())).thenReturn(solicitacao);

            useCase.execute(new ReclassificarRiscoCommand(solicitacao.getId(), ERiscoSolicitado.VERDE));

            verify(eventPublisher).publishSolicitacaoReclassificada(solicitacao);
        }
    }

    @Nested
    @DisplayName("quando o status nao e APROVADA")
    class QuandoStatusInvalido {

        @ParameterizedTest(name = "status {0} deve lancar SolicitacaoNaoPendenteException")
        @EnumSource(value = EStatusSolicitacao.class,
                names = {"AGUARDANDO", "NEGADA", "CANCELADA", "DEVOLVIDA", "PENDENTE", "AGENDADA", "ATENDIDA", "FALTOU"})
        void deveLancarParaStatusInvalido(EStatusSolicitacao status) {
            Solicitacao solicitacao = buildSolicitacao(status, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);

            assertThatThrownBy(() -> useCase.execute(new ReclassificarRiscoCommand(solicitacao.getId(), ERiscoSolicitado.VERMELHO)))
                    .isInstanceOf(SolicitacaoNaoPendenteException.class);

            verify(solicitacaoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("quando o destino e FILA_ESPERA")
    class QuandoFilaEspera {

        @Test
        @DisplayName("deve lancar SolicitacaoNaoPendenteException")
        void deveLancarExcecao() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.APROVADA, EDestino.FILA_ESPERA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);

            assertThatThrownBy(() -> useCase.execute(new ReclassificarRiscoCommand(solicitacao.getId(), ERiscoSolicitado.VERMELHO)))
                    .isInstanceOf(SolicitacaoNaoPendenteException.class);

            verify(solicitacaoRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("quando a solicitacao nao existe")
    class QuandoNaoExiste {

        @Test
        @DisplayName("deve propagar SolicitacaoNaoEncontradaException")
        void devePropagar() {
            UUID id = UUID.randomUUID();
            when(solicitacaoRepository.findById(id)).thenThrow(new SolicitacaoNaoEncontradaException("nao encontrada"));

            assertThatThrownBy(() -> useCase.execute(new ReclassificarRiscoCommand(id, ERiscoSolicitado.VERMELHO)))
                    .isInstanceOf(SolicitacaoNaoEncontradaException.class);
        }
    }
}
