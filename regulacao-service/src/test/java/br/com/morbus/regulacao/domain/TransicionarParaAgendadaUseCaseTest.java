package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.domain.usecase.solicitacao.TransicionarParaAgendadaUseCase;
import br.com.morbus.regulacao.ports.in.dto.AppointmentCreatedCommand;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransicionarParaAgendadaUseCase")
class TransicionarParaAgendadaUseCaseTest {

    @Mock
    private ISolicitacaoRepository repository;

    private TransicionarParaAgendadaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new TransicionarParaAgendadaUseCase(repository);
    }

    private Solicitacao buildSolicitacao(EStatusSolicitacao status) {
        return new Solicitacao(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                status,
                ERiscoSolicitado.AZUL,
                "I10",
                "justificativa clinica",
                "Dr. Silva",
                "CRM-12345",
                EDestino.FILA_REGULADA,
                null,
                UUID.randomUUID(),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusHours(1),
                null
        );
    }

    @Nested
    @DisplayName("quando a solicitacao esta APROVADA")
    class QuandoAprovada {

        @Test
        @DisplayName("deve transicionar o status para AGENDADA")
        void deveTransicionarParaAgendada() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.APROVADA);
            UUID appointmentId = UUID.randomUUID();
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);
            when(repository.save(any())).thenReturn(solicitacao);

            useCase.execute(new AppointmentCreatedCommand(solicitacao.getId(), appointmentId, UUID.randomUUID()));

            assertThat(solicitacao.getStatus()).isEqualTo(EStatusSolicitacao.AGENDADA);
        }

        @Test
        @DisplayName("deve persistir o appointmentId recebido")
        void devePersistirAppointmentId() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.APROVADA);
            UUID appointmentId = UUID.randomUUID();
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);
            when(repository.save(any())).thenReturn(solicitacao);

            useCase.execute(new AppointmentCreatedCommand(solicitacao.getId(), appointmentId, UUID.randomUUID()));

            assertThat(solicitacao.getAppointmentId()).isEqualTo(appointmentId);
        }

        @Test
        @DisplayName("deve persistir a solicitacao atualizada")
        void devePersistir() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.APROVADA);
            UUID appointmentId = UUID.randomUUID();
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);
            when(repository.save(any())).thenReturn(solicitacao);

            useCase.execute(new AppointmentCreatedCommand(solicitacao.getId(), appointmentId, UUID.randomUUID()));

            verify(repository).save(solicitacao);
        }
    }

    @Nested
    @DisplayName("quando a solicitacao nao existe (idempotencia)")
    class QuandoNaoExiste {

        @Test
        @DisplayName("nao deve lancar excecao")
        void naoDeveLancarExcecao() {
            UUID solicitacaoId = UUID.randomUUID();
            when(repository.findById(solicitacaoId)).thenThrow(new SolicitacaoNaoEncontradaException("nao encontrada"));

            useCase.execute(new AppointmentCreatedCommand(solicitacaoId, UUID.randomUUID(), UUID.randomUUID()));
        }

        @Test
        @DisplayName("nao deve salvar quando a solicitacao nao existe")
        void naoDeveSalvar() {
            UUID solicitacaoId = UUID.randomUUID();
            when(repository.findById(solicitacaoId)).thenThrow(new SolicitacaoNaoEncontradaException("nao encontrada"));

            useCase.execute(new AppointmentCreatedCommand(solicitacaoId, UUID.randomUUID(), UUID.randomUUID()));

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("quando o status nao e APROVADA")
    class QuandoStatusNaoPermitido {

        @ParameterizedTest(name = "status {0} deve descartar sem lancar excecao")
        @EnumSource(value = EStatusSolicitacao.class,
                names = {"AGUARDANDO", "NEGADA", "CANCELADA", "DEVOLVIDA", "PENDENTE", "AGENDADA", "ATENDIDA", "FALTOU"})
        @DisplayName("nao deve lancar excecao para status invalidos")
        void naoDeveLancarExcecao(EStatusSolicitacao status) {
            Solicitacao solicitacao = buildSolicitacao(status);
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);

            useCase.execute(new AppointmentCreatedCommand(solicitacao.getId(), UUID.randomUUID(), UUID.randomUUID()));
        }

        @Test
        @DisplayName("nao deve salvar quando o status nao permite a transicao")
        void naoDeveSalvar() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGUARDANDO);
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);

            useCase.execute(new AppointmentCreatedCommand(solicitacao.getId(), UUID.randomUUID(), UUID.randomUUID()));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("nao deve alterar o status original")
        void naoDeveAlterarStatus() {
            Solicitacao solicitacao = buildSolicitacao(EStatusSolicitacao.AGENDADA);
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);

            useCase.execute(new AppointmentCreatedCommand(solicitacao.getId(), UUID.randomUUID(), UUID.randomUUID()));

            assertThat(solicitacao.getStatus()).isEqualTo(EStatusSolicitacao.AGENDADA);
        }
    }
}
