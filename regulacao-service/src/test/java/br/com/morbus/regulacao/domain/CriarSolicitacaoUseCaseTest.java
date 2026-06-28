package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.exception.DuplicateSolicitacaoException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
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

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private CriarSolicitacaoCommand buildCommand() {
        return new CriarSolicitacaoCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                ERiscoSolicitado.AMARELO,
                "Paciente com dores persistentes",
                UUID.randomUUID()
        );
    }

    private Solicitacao buildSolicitacaoSalva(CriarSolicitacaoCommand cmd) {
        return new Solicitacao(
                cmd.pacienteId(),
                cmd.procedureId(),
                cmd.unidadeSolicitanteId(),
                cmd.riscoSolicitado(),
                cmd.observacoes(),
                cmd.solicitadoPor()
        );
    }

    // ── Fluxo feliz ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando não existe solicitação ativa")
    class QuandoNaoExisteDuplicata {

        @Test
        @DisplayName("deve retornar a solicitação com status PENDENTE")
        void deveRetornarComStatusPendente() {
            CriarSolicitacaoCommand cmd = buildCommand();
            Solicitacao salva = buildSolicitacaoSalva(cmd);
            when(repository.existsAtiva(cmd.pacienteId(), cmd.procedureId())).thenReturn(false);
            when(repository.save(any())).thenReturn(salva);

            Solicitacao result = useCase.execute(cmd);

            assertThat(result.getStatus().name()).isEqualTo("PENDENTE");
        }

        @Test
        @DisplayName("deve persistir a solicitação no repositório")
        void devePersistir() {
            CriarSolicitacaoCommand cmd = buildCommand();
            when(repository.existsAtiva(cmd.pacienteId(), cmd.procedureId())).thenReturn(false);
            when(repository.save(any())).thenReturn(buildSolicitacaoSalva(cmd));

            useCase.execute(cmd);

            verify(repository).save(any(Solicitacao.class));
        }

        @Test
        @DisplayName("deve retornar a entidade devolvida pelo repositório")
        void deveRetornarEntidadeDoRepositorio() {
            CriarSolicitacaoCommand cmd = buildCommand();
            Solicitacao salva = buildSolicitacaoSalva(cmd);
            when(repository.existsAtiva(cmd.pacienteId(), cmd.procedureId())).thenReturn(false);
            when(repository.save(any())).thenReturn(salva);

            Solicitacao result = useCase.execute(cmd);

            assertThat(result).isSameAs(salva);
        }

        @Test
        @DisplayName("deve verificar duplicata antes de salvar")
        void deveVerificarDuplicataAntesDeSalvar() {
            CriarSolicitacaoCommand cmd = buildCommand();
            when(repository.existsAtiva(cmd.pacienteId(), cmd.procedureId())).thenReturn(false);
            when(repository.save(any())).thenReturn(buildSolicitacaoSalva(cmd));

            useCase.execute(cmd);

            var order = inOrder(repository);
            order.verify(repository).existsAtiva(cmd.pacienteId(), cmd.procedureId());
            order.verify(repository).save(any());
        }
    }

    // ── Duplicata ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando já existe solicitação PENDENTE ou APROVADA")
    class QuandoExisteDuplicata {

        @Test
        @DisplayName("deve lançar DuplicateSolicitacaoException")
        void deveLancarDuplicateSolicitacaoException() {
            CriarSolicitacaoCommand cmd = buildCommand();
            when(repository.existsAtiva(cmd.pacienteId(), cmd.procedureId())).thenReturn(true);

            assertThatThrownBy(() -> useCase.execute(cmd))
                    .isInstanceOf(DuplicateSolicitacaoException.class);
        }

        @Test
        @DisplayName("não deve persistir quando há duplicata")
        void naoDevePersistir() {
            CriarSolicitacaoCommand cmd = buildCommand();
            when(repository.existsAtiva(cmd.pacienteId(), cmd.procedureId())).thenReturn(true);

            assertThatThrownBy(() -> useCase.execute(cmd))
                    .isInstanceOf(DuplicateSolicitacaoException.class);

            verify(repository, never()).save(any());
        }
    }
}
