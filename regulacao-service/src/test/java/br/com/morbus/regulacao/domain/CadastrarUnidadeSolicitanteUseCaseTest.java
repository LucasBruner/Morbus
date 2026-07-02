package br.com.morbus.regulacao.domain;

import br.com.morbus.regulacao.domain.exception.UnidadeSolicitanteDuplicadaException;
import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;
import br.com.morbus.regulacao.domain.usecase.unidade.CadastrarUnidadeSolicitanteUseCase;
import br.com.morbus.regulacao.ports.in.dto.CadastrarUnidadeSolicitanteCommand;
import br.com.morbus.regulacao.ports.out.IUnidadeSolicitanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CadastrarUnidadeSolicitanteUseCase")
class CadastrarUnidadeSolicitanteUseCaseTest {

    @Mock
    private IUnidadeSolicitanteRepository unidadeSolicitanteRepository;

    private CadastrarUnidadeSolicitanteUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CadastrarUnidadeSolicitanteUseCase(unidadeSolicitanteRepository);
    }

    @Test
    @DisplayName("deve persistir a unidade com os dados informados")
    void devePersistirComDadosInformados() {
        CadastrarUnidadeSolicitanteCommand cmd = new CadastrarUnidadeSolicitanteCommand(
                "1234567", "UBS Central", "Rua das Flores, 100", "(11) 4444-5555");
        when(unidadeSolicitanteRepository.salvar(any(UnidadeSolicitante.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UnidadeSolicitante result = useCase.execute(cmd);

        assertThat(result.getCnes()).isEqualTo("1234567");
        assertThat(result.getNome()).isEqualTo("UBS Central");
        assertThat(result.getEndereco()).isEqualTo("Rua das Flores, 100");
        assertThat(result.getTelefone()).isEqualTo("(11) 4444-5555");
    }

    @Test
    @DisplayName("deve gerar um novo id para a unidade cadastrada")
    void deveGerarNovoId() {
        CadastrarUnidadeSolicitanteCommand cmd = new CadastrarUnidadeSolicitanteCommand(
                "1234567", "UBS Central", "Rua das Flores, 100", "(11) 4444-5555");
        when(unidadeSolicitanteRepository.salvar(any(UnidadeSolicitante.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UnidadeSolicitante result = useCase.execute(cmd);

        assertThat(result.getId()).isNotNull();
    }

    @Test
    @DisplayName("deve retornar a entidade devolvida pelo repositorio")
    void deveRetornarEntidadeDoRepositorio() {
        CadastrarUnidadeSolicitanteCommand cmd = new CadastrarUnidadeSolicitanteCommand(
                "1234567", "UBS Central", "Rua das Flores, 100", "(11) 4444-5555");
        UnidadeSolicitante salva = new UnidadeSolicitante("1234567", "UBS Central", "Rua das Flores, 100", "(11) 4444-5555");
        when(unidadeSolicitanteRepository.salvar(any(UnidadeSolicitante.class))).thenReturn(salva);

        UnidadeSolicitante result = useCase.execute(cmd);

        assertThat(result).isSameAs(salva);
        ArgumentCaptor<UnidadeSolicitante> captor = ArgumentCaptor.forClass(UnidadeSolicitante.class);
        verify(unidadeSolicitanteRepository).salvar(captor.capture());
        assertThat(captor.getValue().getCnes()).isEqualTo("1234567");
    }

    @Test
    @DisplayName("deve lancar excecao quando ja existe unidade com o mesmo cnes")
    void deveLancarExcecaoQuandoCnesDuplicado() {
        CadastrarUnidadeSolicitanteCommand cmd = new CadastrarUnidadeSolicitanteCommand(
                "1234567", "UBS Central", "Rua das Flores, 100", "(11) 4444-5555");
        when(unidadeSolicitanteRepository.existsByCnes("1234567")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(cmd))
                .isInstanceOf(UnidadeSolicitanteDuplicadaException.class)
                .hasMessageContaining("1234567");

        verify(unidadeSolicitanteRepository, never()).salvar(any());
    }
}
