package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.dto.PageResult;
import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.domain.usecase.quota.ConsultarCotasUseCase;
import br.com.morbus.regulacao.ports.in.dto.ConsultarCotasQuery;
import br.com.morbus.regulacao.ports.out.IQuotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarCotasUseCase")
class ConsultarCotasUseCaseTest {

    @Mock
    private IQuotaRepository quotaRepository;

    private ConsultarCotasUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarCotasUseCase(quotaRepository);
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("deve delegar a query recebida diretamente para o repositorio")
        void deveDelegarQueryParaRepositorio() {
            ConsultarCotasQuery query = new ConsultarCotasQuery(
                    UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 7, 1), 0, 20);
            when(quotaRepository.listar(query)).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

            useCase.execute(query);

            verify(quotaRepository).listar(query);
        }

        @Test
        @DisplayName("deve funcionar com filtros opcionais nulos")
        void deveFuncionarComFiltrosNulos() {
            ConsultarCotasQuery query = new ConsultarCotasQuery(null, null, null, 0, 20);
            when(quotaRepository.listar(query)).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

            PageResult<Quota> result = useCase.execute(query);

            assertThat(result.content()).isEmpty();
            verify(quotaRepository).listar(query);
        }

        @Test
        @DisplayName("deve retornar a pagina de cotas devolvida pelo repositorio")
        void deveRetornarPaginaDoRepositorio() {
            Quota quota = new Quota(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    10, 3, LocalDate.of(2026, 7, 1));
            ConsultarCotasQuery query = new ConsultarCotasQuery(null, null, null, 0, 20);
            PageResult<Quota> pagina = new PageResult<>(List.of(quota), 0, 20, 1, 1);
            when(quotaRepository.listar(query)).thenReturn(pagina);

            PageResult<Quota> result = useCase.execute(query);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().getId()).isEqualTo(quota.getId());
        }
    }
}