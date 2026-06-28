package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SolicitacaoCreatedResponseDTO")
class SolicitacaoCreatedResponseDTOTest {

    @Test
    @DisplayName("fromDomain deve mapear todos os campos da solicitação")
    void fromDomainMapeiaCorretamente() {
        UUID id = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        UUID procedureId = UUID.randomUUID();
        UUID unidadeId = UUID.randomUUID();
        UUID solicitadoPor = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(5);

        Solicitacao s = new Solicitacao(id, pacienteId, procedureId, unidadeId, null,
                EStatusSolicitacao.PENDENTE, ERiscoSolicitado.VERDE, "obs",
                null, solicitadoPor, createdAt, createdAt);

        SolicitacaoCreatedResponseDTO dto = SolicitacaoCreatedResponseDTO.fromDomain(s);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.pacienteId()).isEqualTo(pacienteId);
        assertThat(dto.procedureId()).isEqualTo(procedureId);
        assertThat(dto.unidadeSolicitanteId()).isEqualTo(unidadeId);
        assertThat(dto.status()).isEqualTo(EStatusSolicitacao.PENDENTE);
        assertThat(dto.riscoSolicitado()).isEqualTo(ERiscoSolicitado.VERDE);
        assertThat(dto.observacoes()).isEqualTo("obs");
        assertThat(dto.solicitadoPor()).isEqualTo(solicitadoPor);
        assertThat(dto.createdAt()).isEqualTo(createdAt);
    }
}
