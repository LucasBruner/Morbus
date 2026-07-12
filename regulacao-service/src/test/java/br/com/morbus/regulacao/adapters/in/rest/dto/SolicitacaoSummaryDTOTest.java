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

@DisplayName("SolicitacaoSummaryDTO")
class SolicitacaoSummaryDTOTest {

    @Test
    @DisplayName("fromDomain deve mapear todos os campos corretamente")
    void fromDomainMapeiaCorretamente() {
        UUID id = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID procedureId = UUID.randomUUID();
        UUID unidadeId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusHours(1);

        Solicitacao s = new Solicitacao(id, patientId, procedureId, unidadeId, null,
                EStatusSolicitacao.AGUARDANDO, ERiscoSolicitado.AZUL,
                "I10", "Hipertensao grave", "Dr. Silva", null,
                EDestino.FILA_REGULADA, null, UUID.randomUUID(), createdAt, createdAt, null, null);

        SolicitacaoSummaryDTO dto = SolicitacaoSummaryDTO.fromDomain(s);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.patientId()).isEqualTo(patientId);
        assertThat(dto.procedureId()).isEqualTo(procedureId);
        assertThat(dto.unitSolicitanteId()).isEqualTo(unidadeId);
        assertThat(dto.status()).isEqualTo(EStatusSolicitacao.AGUARDANDO);
        assertThat(dto.riskColor()).isEqualTo(ERiscoSolicitado.AZUL);
        assertThat(dto.destino()).isEqualTo(EDestino.FILA_REGULADA);
        assertThat(dto.criadaEm()).isEqualTo(createdAt);
    }
}
