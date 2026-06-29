package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.ports.in.dto.CriarSolicitacaoCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SolicitacaoRequestDTO")
class SolicitacaoRequestDTOTest {

    @Test
    @DisplayName("toCommand deve mapear todos os campos corretamente")
    void toCommandMapeiaCorretamente() {
        UUID pacienteId = UUID.randomUUID();
        UUID procedureId = UUID.randomUUID();
        UUID unidadeId = UUID.randomUUID();
        UUID solicitadoPor = UUID.randomUUID();

        SolicitacaoRequestDTO dto = new SolicitacaoRequestDTO(
                pacienteId, procedureId, unidadeId, ERiscoSolicitado.AMARELO, "obs");

        CriarSolicitacaoCommand cmd = dto.toCommand(solicitadoPor);

        assertThat(cmd.pacienteId()).isEqualTo(pacienteId);
        assertThat(cmd.procedureId()).isEqualTo(procedureId);
        assertThat(cmd.unidadeSolicitanteId()).isEqualTo(unidadeId);
        assertThat(cmd.riscoSolicitado()).isEqualTo(ERiscoSolicitado.AMARELO);
        assertThat(cmd.observacoes()).isEqualTo("obs");
        assertThat(cmd.solicitadoPor()).isEqualTo(solicitadoPor);
    }

    @Test
    @DisplayName("toCommand deve preservar campos opcionais nulos")
    void toCommandPreservaNulos() {
        UUID solicitadoPor = UUID.randomUUID();
        SolicitacaoRequestDTO dto = new SolicitacaoRequestDTO(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, null);

        CriarSolicitacaoCommand cmd = dto.toCommand(solicitadoPor);

        assertThat(cmd.riscoSolicitado()).isNull();
        assertThat(cmd.observacoes()).isNull();
    }
}
