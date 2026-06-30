package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.enums.EDecisaoRegulador;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.ports.in.dto.AvaliarSolicitacaoCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AvaliarSolicitacaoRequestDTO(
        @NotNull(message = "decisao e obrigatoria") EDecisaoRegulador decisao,
        ERiscoSolicitado riskColorDefinido,
        String justificativa,
        UUID unidadeExecutanteId) {

    public AvaliarSolicitacaoCommand toCommand(UUID solicitacaoId, UUID reguladorId) {
        return new AvaliarSolicitacaoCommand(
                solicitacaoId, reguladorId, decisao, riskColorDefinido, justificativa, unidadeExecutanteId);
    }
}
