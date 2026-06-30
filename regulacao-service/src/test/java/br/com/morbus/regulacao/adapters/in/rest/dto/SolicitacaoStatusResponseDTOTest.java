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

@DisplayName("SolicitacaoStatusResponseDTO")
class SolicitacaoStatusResponseDTOTest {

    @Test
    @DisplayName("fromDomain deve mapear todos os campos corretamente")
    void fromDomainMapeiaCorretamente() {
        UUID id = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID procedureId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(10);
        LocalDateTime updatedAt = LocalDateTime.now().minusMinutes(1);

        Solicitacao s = new Solicitacao(id, patientId, procedureId, UUID.randomUUID(), null,
                EStatusSolicitacao.APROVADA, ERiscoSolicitado.AMARELO,
                "I10", "Hipertensao grave", "Dr. Silva", null,
                EDestino.FILA_REGULADA, null, UUID.randomUUID(), createdAt, updatedAt);

        SolicitacaoStatusResponseDTO dto = SolicitacaoStatusResponseDTO.fromDomain(s);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.patientId()).isEqualTo(patientId);
        assertThat(dto.procedureId()).isEqualTo(procedureId);
        assertThat(dto.cid()).isEqualTo("I10");
        assertThat(dto.justificativaClinica()).isEqualTo("Hipertensao grave");
        assertThat(dto.profissionalSolicitante()).isEqualTo("Dr. Silva");
        assertThat(dto.destino()).isEqualTo(EDestino.FILA_REGULADA);
        assertThat(dto.riskColor()).isEqualTo(ERiscoSolicitado.AMARELO);
        assertThat(dto.status()).isEqualTo(EStatusSolicitacao.APROVADA);
        assertThat(dto.criadaEm()).isEqualTo(createdAt);
        assertThat(dto.updatedAt()).isEqualTo(updatedAt);
        assertThat(dto.justificativaNegacao()).isNull();
    }

    @Test
    @DisplayName("fromDomain deve retornar justificativaNegacao nula quando ausente")
    void fromDomainRetornaJustificativaNegatioNula() {
        Solicitacao s = new Solicitacao(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), null, EStatusSolicitacao.AGUARDANDO,
                ERiscoSolicitado.AZUL, "I10", "texto", "Dr. Silva",
                null, EDestino.FILA_REGULADA, null, UUID.randomUUID(),
                LocalDateTime.now(), LocalDateTime.now());

        SolicitacaoStatusResponseDTO dto = SolicitacaoStatusResponseDTO.fromDomain(s);

        assertThat(dto.justificativaNegacao()).isNull();
    }
}
