package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.enums.EDestino;
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
        UUID patientId = UUID.randomUUID();
        UUID procedureId = UUID.randomUUID();
        UUID unitSolicitanteId = UUID.randomUUID();
        UUID solicitadoPor = UUID.randomUUID();

        SolicitacaoRequestDTO dto = new SolicitacaoRequestDTO(
                patientId, procedureId, "I10",
                "Hipertensao grave", "Dr. Silva", "CRM/SP 12345",
                unitSolicitanteId, EDestino.FILA_REGULADA, "paciente prefere atendimento pela manha");

        CriarSolicitacaoCommand cmd = dto.toCommand(solicitadoPor);

        assertThat(cmd.patientId()).isEqualTo(patientId);
        assertThat(cmd.procedureId()).isEqualTo(procedureId);
        assertThat(cmd.unidadeSolicitanteId()).isEqualTo(unitSolicitanteId);
        assertThat(cmd.cid()).isEqualTo("I10");
        assertThat(cmd.justificativaClinica()).isEqualTo("Hipertensao grave");
        assertThat(cmd.profissionalSolicitante()).isEqualTo("Dr. Silva");
        assertThat(cmd.crmProfissional()).isEqualTo("CRM/SP 12345");
        assertThat(cmd.destino()).isEqualTo(EDestino.FILA_REGULADA);
        assertThat(cmd.solicitadoPor()).isEqualTo(solicitadoPor);
        assertThat(cmd.observacoes()).isEqualTo("paciente prefere atendimento pela manha");
    }

    @Test
    @DisplayName("toCommand deve preservar crmProfissional nulo")
    void toCommandPreservaCrmNulo() {
        UUID solicitadoPor = UUID.randomUUID();
        SolicitacaoRequestDTO dto = new SolicitacaoRequestDTO(
                UUID.randomUUID(), UUID.randomUUID(), "J45",
                "Asma persistente", "Dra. Costa", null,
                UUID.randomUUID(), EDestino.FILA_ESPERA, null);

        CriarSolicitacaoCommand cmd = dto.toCommand(solicitadoPor);

        assertThat(cmd.crmProfissional()).isNull();
        assertThat(cmd.destino()).isEqualTo(EDestino.FILA_ESPERA);
    }
}
