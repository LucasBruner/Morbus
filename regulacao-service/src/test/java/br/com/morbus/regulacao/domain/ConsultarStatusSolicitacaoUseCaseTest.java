package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.dto.UsuarioContexto;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.IdPacienteIncorretoException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
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

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Solicitacao buildSolicitacao(UUID pacienteId) {
        return new Solicitacao(
                UUID.randomUUID(),
                pacienteId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                EStatusSolicitacao.PENDENTE,
                ERiscoSolicitado.AMARELO,
                "observação",
                null,
                UUID.randomUUID(),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusHours(1)
        );
    }

    // ── MEDICO ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando role é ROLE_MEDICO")
    class QuandoMedico {

        @Test
        @DisplayName("deve retornar a solicitação sem restrição de pacienteId")
        void deveRetornarSemRestricao() {
            Solicitacao solicitacao = buildSolicitacao(UUID.randomUUID());
            UsuarioContexto contexto = new UsuarioContexto("ROLE_MEDICO", UUID.randomUUID());
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);

            Solicitacao result = useCase.execute(solicitacao.getId(), contexto);

            assertThat(result).isSameAs(solicitacao);
        }
    }

    // ── SOLICITANTE ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando role é ROLE_SOLICITANTE")
    class QuandoSolicitante {

        @Test
        @DisplayName("deve retornar a solicitação sem restrição de pacienteId")
        void deveRetornarSemRestricao() {
            Solicitacao solicitacao = buildSolicitacao(UUID.randomUUID());
            UsuarioContexto contexto = new UsuarioContexto("ROLE_SOLICITANTE", UUID.randomUUID());
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);

            Solicitacao result = useCase.execute(solicitacao.getId(), contexto);

            assertThat(result).isSameAs(solicitacao);
        }
    }

    // ── PACIENTE ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando role é ROLE_PACIENTE")
    class QuandoPaciente {

        @Test
        @DisplayName("deve retornar a solicitação quando pacienteId bate com o do JWT")
        void deveRetornarQuandoPacienteIdCorreto() {
            UUID pacienteId = UUID.randomUUID();
            Solicitacao solicitacao = buildSolicitacao(pacienteId);
            UsuarioContexto contexto = new UsuarioContexto("ROLE_PACIENTE", pacienteId);
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);

            Solicitacao result = useCase.execute(solicitacao.getId(), contexto);

            assertThat(result).isSameAs(solicitacao);
        }

        @Test
        @DisplayName("deve lançar IdPacienteIncorretoException quando pacienteId não bate com o do JWT")
        void deveLancarIdPacienteIncorretoException() {
            Solicitacao solicitacao = buildSolicitacao(UUID.randomUUID());
            UsuarioContexto contexto = new UsuarioContexto("ROLE_PACIENTE", UUID.randomUUID());
            when(repository.findById(solicitacao.getId())).thenReturn(solicitacao);

            assertThatThrownBy(() -> useCase.execute(solicitacao.getId(), contexto))
                    .isInstanceOf(IdPacienteIncorretoException.class);
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
            UsuarioContexto contexto = new UsuarioContexto("ROLE_MEDICO", UUID.randomUUID());
            when(repository.findById(id)).thenThrow(new SolicitacaoNaoEncontradaException("não encontrada"));

            assertThatThrownBy(() -> useCase.execute(id, contexto))
                    .isInstanceOf(SolicitacaoNaoEncontradaException.class);
        }
    }
}