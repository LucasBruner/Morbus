package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.ports.in.dto.ReclassificarRiscoCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReclassificarRiscoRequestDTO(
        @NotNull(message = "riskColor e obrigatorio") ERiscoSolicitado riskColor) {

    public ReclassificarRiscoCommand toCommand(UUID solicitacaoId) {
        return new ReclassificarRiscoCommand(solicitacaoId, riskColor);
    }
}
