package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.DuplicateSolicitacaoException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.domain.usecase.solicitacao.CriarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.dto.CriarSolicitacaoCommand;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CriarSolicitacaoUseCase")
class CriarSolicitacaoUseCaseTest {

    @Mock
    private ISolicitacaoRepository repository;

    private CriarSolicitacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CriarSolicitacaoUseCase(repository);
    }

    private CriarSolicitacaoCommand buildCommand() {
        return new CriarSolicitacaoCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "I10",
                "Paciente com hipertensao grave",
                "Dr. Silva",
                "CRM/SP 12345",
                EDestino.FILA_REGULADA,
                UUID.randomUUID()
        );
    }

    private Solicitacao buildSolicitacaoSalva(CriarSolicitacaoCommand cmd) {
        return new Solicitacao(
                cmd.patientId(),
                cmd.procedureId(),
                cmd.unidadeSolicitanteId(),
                cmd.cid(),
                cmd.justificativaClinica(),
                cmd.profissionalSolicitante(),
                cmd.crmProfissional(),
                cmd.destino(),
                cmd.solicitadoPor()
        );
    }

    @Nested
    @DisplayName("quando nao existe solicitacao ativa")
    class QuandoNaoExisteDuplicata {

        @Test
        @DisplayName("deve retornar a solicitacao com status AGUARDANDO")
        void deveRetornarComStatusAguardando() {
            CriarSolicitacaoCommand cmd = buildCommand();
            Solicitacao salva = buildSolicitacaoSalva(cmd);
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(repository.save(any())).thenReturn(salva);

            Solicitacao result = useCase.execute(cmd);

            assertThat(result.getStatus()).isEqualTo(EStatusSolicitacao.AGUARDANDO);
        }

        @Test
        @DisplayName("deve persistir a solicitacao no repositorio")
        void devePersistir() {
            CriarSolicitacaoCommand cmd = buildCommand();
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(repository.save(any())).thenReturn(buildSolicitacaoSalva(cmd));

            useCase.execute(cmd);

            verify(repository).save(any(Solicitacao.class));
        }

        @Test
        @DisplayName("deve retornar a entidade devolvida pelo repositorio")
        void deveRetornarEntidadeDoRepositorio() {
            CriarSolicitacaoCommand cmd = buildCommand();
            Solicitacao salva = buildSolicitacaoSalva(cmd);
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(repository.save(any())).thenReturn(salva);

            Solicitacao result = useCase.execute(cmd);

            assertThat(result).isSameAs(salva);
        }

        @Test
        @DisplayName("deve verificar duplicata antes de salvar")
        void deveVerificarDuplicataAntesDeSalvar() {
            CriarSolicitacaoCommand cmd = buildCommand();
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(false);
            when(repository.save(any())).thenReturn(buildSolicitacaoSalva(cmd));

            useCase.execute(cmd);

            var order = inOrder(repository);
            order.verify(repository).existsAtiva(cmd.patientId(), cmd.procedureId());
            order.verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("quando ja existe solicitacao AGUARDANDO ou APROVADA")
    class QuandoExisteDuplicata {

        @Test
        @DisplayName("deve lancar DuplicateSolicitacaoException")
        void deveLancarDuplicateSolicitacaoException() {
            CriarSolicitacaoCommand cmd = buildCommand();
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(true);

            assertThatThrownBy(() -> useCase.execute(cmd))
                    .isInstanceOf(DuplicateSolicitacaoException.class);
        }

        @Test
        @DisplayName("nao deve persistir quando ha duplicata")
        void naoDevePersistir() {
            CriarSolicitacaoCommand cmd = buildCommand();
            when(repository.existsAtiva(cmd.patientId(), cmd.procedureId())).thenReturn(true);

            assertThatThrownBy(() -> useCase.execute(cmd))
                    .isInstanceOf(DuplicateSolicitacaoException.class);

            verify(repository, never()).save(any());
        }
    }
}
