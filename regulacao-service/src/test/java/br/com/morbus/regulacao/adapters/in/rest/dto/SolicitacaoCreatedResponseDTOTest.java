package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.enums.EDestino;
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
    @DisplayName("fromDomain deve mapear todos os campos da solicitacao")
    void fromDomainMapeiaCorretamente() {
        UUID id = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID procedureId = UUID.randomUUID();
        UUID unidadeId = UUID.randomUUID();
        UUID solicitadoPor = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(5);

        Solicitacao s = new Solicitacao(id, patientId, procedureId, unidadeId, null,
                EStatusSolicitacao.AGUARDANDO, ERiscoSolicitado.AZUL,
                "I10", "Hipertensao grave", "Dr. Silva", null,
                EDestino.FILA_REGULADA, null, solicitadoPor, createdAt, createdAt);

        SolicitacaoCreatedResponseDTO dto = SolicitacaoCreatedResponseDTO.fromDomain(s);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.patientId()).isEqualTo(patientId);
        assertThat(dto.procedureId()).isEqualTo(procedureId);
        assertThat(dto.cid()).isEqualTo("I10");
        assertThat(dto.destino()).isEqualTo(EDestino.FILA_REGULADA);
        assertThat(dto.riskColor()).isEqualTo(ERiscoSolicitado.AZUL);
        assertThat(dto.status()).isEqualTo(EStatusSolicitacao.AGUARDANDO);
        assertThat(dto.criadaEm()).isEqualTo(createdAt);
    }
}
