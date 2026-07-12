package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoPendenteException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.domain.usecase.solicitacao.ComplementarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.dto.ComplementarSolicitacaoCommand;
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
@DisplayName("ComplementarSolicitacaoUseCase")
class ComplementarSolicitacaoUseCaseTest {

    @Mock
    private ISolicitacaoRepository solicitacaoRepository;

    private ComplementarSolicitacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ComplementarSolicitacaoUseCase(solicitacaoRepository);
    }

    private Solicitacao buildSolicitacao(EStatusSolicitacao status) {
        return new Solicitacao(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                status, ERiscoSolicitado.AZUL, "I10", "justificativa clinica antiga", "Dr. Silva", null,
                EDestino.FILA_REGULADA, "faltou informacao clinica", UUID.randomUUID(),
                LocalDateTime.now().minusHours(1), LocalDateTime.now().minusHours(1), null, null
        );
    }

    @Nested
    @DisplayName("quando a solicitacao esta DEVOLVIDA")
    class QuandoDevolvida {

        @Test
        @DisplayName("deve atualizar apenas os campos nao nulos enviados")
        void deveAtualizarApenasCamposNaoNulos() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.DEVOLVIDA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            when(solicitacaoRepository.save(any())).thenReturn(solicitacao);

            ComplementarSolicitacaoCommand command = new ComplementarSolicitacaoCommand(
                    solicitacao.getId(), "I11.9", "Complemento: paciente com HAS estagio 3", null, "CRM/SP 12345",
                    "paciente confirmou disponibilidade pela manha");

            Solicitacao result = useCase.execute(command);

            assertThat(result.getCid()).isEqualTo("I11.9");
            assertThat(result.getJustificativaClinica()).isEqualTo("Complemento: paciente com HAS estagio 3");
            assertThat(result.getProfissionalSolicitante()).isEqualTo("Dr. Silva");
            assertThat(result.getCrmProfissional()).isEqualTo("CRM/SP 12345");
            assertThat(result.getObservacoes()).isEqualTo("paciente confirmou disponibilidade pela manha");
        }

        @Test
        @DisplayName("deve voltar o status para AGUARDANDO")
        void deveVoltarParaAguardando() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.DEVOLVIDA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            when(solicitacaoRepository.save(any())).thenReturn(solicitacao);

            Solicitacao result = useCase.execute(new ComplementarSolicitacaoCommand(
                    solicitacao.getId(), "I11.9", null, null, null, null));

            assertThat(result.getStatus()).isEqualTo(EStatusSolicitacao.AGUARDANDO);
        }

        @Test
        @DisplayName("deve limpar a justificativaNegacao")
        void deveLimparJustificativaNegacao() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.DEVOLVIDA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            when(solicitacaoRepository.save(any())).thenReturn(solicitacao);

            Solicitacao result = useCase.execute(new ComplementarSolicitacaoCommand(
                    solicitacao.getId(), null, null, null, null, null));

            assertThat(result.getJustificativaNegacao()).isNull();
        }

        @Test
        @DisplayName("deve persistir a solicitacao complementada")
        void devePersistir() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.DEVOLVIDA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            when(solicitacaoRepository.save(any())).thenReturn(solicitacao);

            useCase.execute(new ComplementarSolicitacaoCommand(solicitacao.getId(), "I11.9", null, null, null, null));

            verify(solicitacaoRepository).save(solicitacao);
        }
    }

    @Nested
    @DisplayName("quando o status nao e DEVOLVIDA")
    class QuandoStatusInvalido {

        @ParameterizedTest(name = "status {0} deve lancar SolicitacaoNaoPendenteException")
        @EnumSource(value = EStatusSolicitacao.class,
                names = {"AGUARDANDO", "APROVADA", "NEGADA", "CANCELADA", "PENDENTE", "AGENDADA", "ATENDIDA", "FALTOU"})
        void deveLancarParaStatusInvalido(EStatusSolicitacao status) {
            Solicitacao solicitacao = buildSolicitacao(status);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);

            assertThatThrownBy(() -> useCase.execute(new ComplementarSolicitacaoCommand(
                    solicitacao.getId(), "I11.9", null, null, null, null)))
                    .isInstanceOf(SolicitacaoNaoPendenteException.class);

            verify(solicitacaoRepository, never()).save(any());
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

            assertThatThrownBy(() -> useCase.execute(new ComplementarSolicitacaoCommand(id, "I11.9", null, null, null, null)))
                    .isInstanceOf(SolicitacaoNaoEncontradaException.class);
        }
    }
}
