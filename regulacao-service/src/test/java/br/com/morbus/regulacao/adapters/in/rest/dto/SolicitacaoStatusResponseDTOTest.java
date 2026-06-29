package br.com.morbus.regulacao.adapters.in.rest.dto;

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
        UUID pacienteId = UUID.randomUUID();
        UUID procedureId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(10);
        LocalDateTime updatedAt = LocalDateTime.now().minusMinutes(1);

        Solicitacao s = new Solicitacao(id, pacienteId, procedureId, UUID.randomUUID(), null,
                EStatusSolicitacao.APROVADA, ERiscoSolicitado.VERMELHO, "obs",
                "Aprovado pelo regulador", UUID.randomUUID(), createdAt, updatedAt);

        SolicitacaoStatusResponseDTO dto = SolicitacaoStatusResponseDTO.fromDomain(s);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.pacienteId()).isEqualTo(pacienteId);
        assertThat(dto.procedureId()).isEqualTo(procedureId);
        assertThat(dto.statusSolicitacao()).isEqualTo(EStatusSolicitacao.APROVADA);
        assertThat(dto.riscoSolicitado()).isEqualTo(ERiscoSolicitado.VERMELHO);
        assertThat(dto.createdAt()).isEqualTo(createdAt);
        assertThat(dto.updatedAt()).isEqualTo(updatedAt);
        assertThat(dto.parecer()).isEqualTo("Aprovado pelo regulador");
    }

    @Test
    @DisplayName("fromDomain deve retornar parecer nulo quando justificativa não existe")
    void fromDomainRetornaParecerNulo() {
        Solicitacao s = new Solicitacao(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), null, EStatusSolicitacao.PENDENTE, null, null,
                null, UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now());

        SolicitacaoStatusResponseDTO dto = SolicitacaoStatusResponseDTO.fromDomain(s);

        assertThat(dto.parecer()).isNull();
    }
}
