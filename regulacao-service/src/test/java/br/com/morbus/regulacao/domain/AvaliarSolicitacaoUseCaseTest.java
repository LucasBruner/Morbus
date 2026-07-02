package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.enums.EDecisaoRegulador;
import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.CampoObrigatorioException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoPendenteException;
import br.com.morbus.regulacao.domain.model.Parecer;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.domain.usecase.solicitacao.AvaliarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.dto.AvaliarSolicitacaoCommand;
import br.com.morbus.regulacao.ports.in.dto.AvaliarSolicitacaoResult;
import br.com.morbus.regulacao.ports.out.IParecerRepository;
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
@DisplayName("AvaliarSolicitacaoUseCase")
class AvaliarSolicitacaoUseCaseTest {

    @Mock
    private ISolicitacaoRepository solicitacaoRepository;

    @Mock
    private IParecerRepository parecerRepository;

    @Mock
    private IRegulacaoEventPublisher eventPublisher;

    private AvaliarSolicitacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AvaliarSolicitacaoUseCase(solicitacaoRepository, parecerRepository, eventPublisher);
    }

    private Solicitacao buildSolicitacao(EStatusSolicitacao status, EDestino destino) {
        return new Solicitacao(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                status, ERiscoSolicitado.AZUL, "I10", "justificativa clinica", "Dr. Silva", null,
                destino, null, UUID.randomUUID(),
                LocalDateTime.now().minusHours(1), LocalDateTime.now().minusHours(1), null
        );
    }

    private AvaliarSolicitacaoCommand buildCommand(UUID solicitacaoId, EDecisaoRegulador decisao,
                                                    ERiscoSolicitado riskColor, String justificativa) {
        return new AvaliarSolicitacaoCommand(solicitacaoId, UUID.randomUUID(), decisao, riskColor, justificativa, UUID.randomUUID());
    }

    private void mockPersistencia(Solicitacao solicitacao) {
        when(solicitacaoRepository.save(any())).thenReturn(solicitacao);
        when(parecerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("quando decisao = AUTORIZAR")
    class QuandoAutorizar {

        @Test
        @DisplayName("deve transicionar para APROVADA e definir riskColor e unidadeExecutanteId")
        void deveAprovar() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGUARDANDO, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            mockPersistencia(solicitacao);

            AvaliarSolicitacaoCommand cmd = buildCommand(solicitacao.getId(), EDecisaoRegulador.AUTORIZAR, ERiscoSolicitado.AMARELO, null);

            AvaliarSolicitacaoResult result = useCase.execute(cmd);

            assertThat(solicitacao.getStatus()).isEqualTo(EStatusSolicitacao.APROVADA);
            assertThat(solicitacao.getRiskColor()).isEqualTo(ERiscoSolicitado.AZUL);
            assertThat(solicitacao.getUnidadeExecutanteId()).isEqualTo(cmd.unidadeExecutanteId());
            assertThat(result.novoStatus()).isEqualTo(EStatusSolicitacao.APROVADA);
        }

        @Test
        @DisplayName("deve permitir transicao a partir de PENDENTE")
        void devePermitirAPartirDePendente() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.PENDENTE, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            mockPersistencia(solicitacao);

            useCase.execute(buildCommand(solicitacao.getId(), EDecisaoRegulador.AUTORIZAR, ERiscoSolicitado.VERDE, null));

            assertThat(solicitacao.getStatus()).isEqualTo(EStatusSolicitacao.APROVADA);
        }

        @Test
        @DisplayName("deve publicar solicitation.approved")
        void devePublicarEvento() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGUARDANDO, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            mockPersistencia(solicitacao);

            useCase.execute(buildCommand(solicitacao.getId(), EDecisaoRegulador.AUTORIZAR, ERiscoSolicitado.AMARELO, null));

            verify(eventPublisher).publishSolicitacaoAprovada(solicitacao);
        }

        @Test
        @DisplayName("deve criar parecer com a decisao AUTORIZAR")
        void deveCriarParecer() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGUARDANDO, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            mockPersistencia(solicitacao);

            useCase.execute(buildCommand(solicitacao.getId(), EDecisaoRegulador.AUTORIZAR, ERiscoSolicitado.AMARELO, null));

            verify(parecerRepository).save(argThat((Parecer p) -> p.getDecisao() == EDecisaoRegulador.AUTORIZAR));
        }

        @Test
        @DisplayName("deve lancar CampoObrigatorioException quando riskColorDefinido esta ausente")
        void deveLancarCampoObrigatorioSemRiskColor() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGUARDANDO, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);

            assertThatThrownBy(() -> useCase.execute(buildCommand(solicitacao.getId(), EDecisaoRegulador.AUTORIZAR, null, null)))
                    .isInstanceOf(CampoObrigatorioException.class);

            verify(solicitacaoRepository, never()).save(any());
            verify(parecerRepository, never()).save(any());
        }

        @ParameterizedTest(name = "status {0} deve lancar SolicitacaoNaoPendenteException")
        @EnumSource(value = EStatusSolicitacao.class,
                names = {"APROVADA", "NEGADA", "CANCELADA", "DEVOLVIDA", "AGENDADA", "ATENDIDA", "FALTOU"})
        @DisplayName("deve lancar SolicitacaoNaoPendenteException para status invalidos")
        void deveLancarParaStatusInvalido(EStatusSolicitacao status) {
            Solicitacao solicitacao = buildSolicitacao(status, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);

            assertThatThrownBy(() -> useCase.execute(buildCommand(solicitacao.getId(), EDecisaoRegulador.AUTORIZAR, ERiscoSolicitado.AMARELO, null)))
                    .isInstanceOf(SolicitacaoNaoPendenteException.class);

            verify(solicitacaoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("quando decisao = FILA_ESPERA")
    class QuandoFilaEspera {

        @Test
        @DisplayName("deve aprovar e sobrescrever destino para FILA_ESPERA")
        void deveAprovarParaFilaEspera() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGUARDANDO, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            mockPersistencia(solicitacao);

            useCase.execute(buildCommand(solicitacao.getId(), EDecisaoRegulador.FILA_ESPERA, ERiscoSolicitado.VERMELHO, null));

            assertThat(solicitacao.getStatus()).isEqualTo(EStatusSolicitacao.APROVADA);
            assertThat(solicitacao.getDestino()).isEqualTo(EDestino.FILA_ESPERA);
        }

        @Test
        @DisplayName("deve publicar solicitation.approved")
        void devePublicarEvento() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGUARDANDO, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            mockPersistencia(solicitacao);

            useCase.execute(buildCommand(solicitacao.getId(), EDecisaoRegulador.FILA_ESPERA, ERiscoSolicitado.VERMELHO, null));

            verify(eventPublisher).publishSolicitacaoAprovada(solicitacao);
        }
    }

    @Nested
    @DisplayName("quando decisao = NEGAR")
    class QuandoNegar {

        @Test
        @DisplayName("deve transicionar para NEGADA e definir justificativa")
        void deveNegar() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGUARDANDO, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            mockPersistencia(solicitacao);

            useCase.execute(buildCommand(solicitacao.getId(), EDecisaoRegulador.NEGAR, null, "sem indicacao clinica"));

            assertThat(solicitacao.getStatus()).isEqualTo(EStatusSolicitacao.NEGADA);
            assertThat(solicitacao.getJustificativaNegacao()).isEqualTo("sem indicacao clinica");
        }

        @Test
        @DisplayName("deve lancar CampoObrigatorioException quando justificativa esta ausente")
        void deveLancarCampoObrigatorioSemJustificativa() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGUARDANDO, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);

            assertThatThrownBy(() -> useCase.execute(buildCommand(solicitacao.getId(), EDecisaoRegulador.NEGAR, null, null)))
                    .isInstanceOf(CampoObrigatorioException.class);

            verify(solicitacaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar SolicitacaoNaoPendenteException quando status nao e AGUARDANDO")
        void deveLancarQuandoStatusNaoAguardando() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.PENDENTE, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);

            assertThatThrownBy(() -> useCase.execute(buildCommand(solicitacao.getId(), EDecisaoRegulador.NEGAR, null, "motivo")))
                    .isInstanceOf(SolicitacaoNaoPendenteException.class);
        }

        @Test
        @DisplayName("nao deve propagar exception quando a publicacao do evento falha")
        void naoDevePropagarFalhaDoPublish() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGUARDANDO, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            mockPersistencia(solicitacao);
            doThrow(new RuntimeException("rabbit indisponivel")).when(eventPublisher).publishSolicitacaoNegada(any());

            AvaliarSolicitacaoResult result = useCase.execute(
                    buildCommand(solicitacao.getId(), EDecisaoRegulador.NEGAR, null, "motivo"));

            assertThat(result.novoStatus()).isEqualTo(EStatusSolicitacao.NEGADA);
        }

        @Test
        @DisplayName("deve persistir a negacao mesmo quando a publicacao do evento falha")
        void devePersistirAindaQuePublishFalhe() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGUARDANDO, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            mockPersistencia(solicitacao);
            doThrow(new RuntimeException("rabbit indisponivel")).when(eventPublisher).publishSolicitacaoNegada(any());

            useCase.execute(buildCommand(solicitacao.getId(), EDecisaoRegulador.NEGAR, null, "motivo"));

            verify(solicitacaoRepository).save(solicitacao);
        }
    }

    @Nested
    @DisplayName("quando decisao = DEVOLVER")
    class QuandoDevolver {

        @Test
        @DisplayName("deve transicionar para DEVOLVIDA e publicar solicitation.devolved")
        void deveDevolver() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGUARDANDO, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            mockPersistencia(solicitacao);

            useCase.execute(buildCommand(solicitacao.getId(), EDecisaoRegulador.DEVOLVER, null, "documentacao incompleta"));

            assertThat(solicitacao.getStatus()).isEqualTo(EStatusSolicitacao.DEVOLVIDA);
            verify(eventPublisher).publishSolicitacaoDevolvida(solicitacao);
        }
    }

    @Nested
    @DisplayName("quando decisao = PENDENTE")
    class QuandoPendente {

        @Test
        @DisplayName("deve transicionar para PENDENTE sem publicar evento")
        void deveMarcarPendenteSemEvento() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGUARDANDO, EDestino.FILA_REGULADA);
            when(solicitacaoRepository.findById(solicitacao.getId())).thenReturn(solicitacao);
            mockPersistencia(solicitacao);

            useCase.execute(buildCommand(solicitacao.getId(), EDecisaoRegulador.PENDENTE, null, null));

            assertThat(solicitacao.getStatus()).isEqualTo(EStatusSolicitacao.PENDENTE);
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

            assertThatThrownBy(() -> useCase.execute(buildCommand(id, EDecisaoRegulador.AUTORIZAR, ERiscoSolicitado.AZUL, null)))
                    .isInstanceOf(SolicitacaoNaoEncontradaException.class);
        }
    }
}
