package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.dto.ListarSolicitacoesQuery;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.domain.usecase.solicitacao.ListarSolicitacoesUseCase;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListarSolicitacoesUseCase")
class ListarSolicitacoesUseCaseTest {

    @Mock
    private ISolicitacaoRepository repository;

    private ListarSolicitacoesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListarSolicitacoesUseCase(repository);
    }

    // ── Fluxo feliz ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando chamado com query")
    class QuandoChamado {

        @Test
        @DisplayName("deve delegar ao repositório com a mesma query")
        void deveDelegarAoRepositorio() {
            ListarSolicitacoesQuery query = new ListarSolicitacoesQuery(UUID.randomUUID(), EStatusSolicitacao.AGUARDANDO, UUID.randomUUID(), 0, 20);
            when(repository.listar(query)).thenReturn(Page.empty());

            useCase.execute(query);

            verify(repository).listar(query);
        }

        @Test
        @DisplayName("deve retornar a página devolvida pelo repositório")
        void deveRetornarPaginaDoRepositorio() {
            ListarSolicitacoesQuery query = new ListarSolicitacoesQuery(null, null, null, 0, 20);
            Page<Solicitacao> pagina = new PageImpl<>(List.of());
            when(repository.listar(query)).thenReturn(pagina);

            Page<Solicitacao> result = useCase.execute(query);

            assertThat(result).isSameAs(pagina);
        }

        @Test
        @DisplayName("deve retornar página vazia quando repositório não encontra resultados")
        void deveRetornarPaginaVazia() {
            ListarSolicitacoesQuery query = new ListarSolicitacoesQuery(UUID.randomUUID(), null, null, 0, 20);
            when(repository.listar(query)).thenReturn(Page.empty());

            Page<Solicitacao> result = useCase.execute(query);

            assertThat(result.getContent()).isEmpty();
        }
    }
}