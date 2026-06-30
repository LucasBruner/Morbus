package br.com.morbus.regulacao.ports.in.dto;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;

import java.util.UUID;

public record ReclassificarRiscoCommand(UUID solicitacaoId, ERiscoSolicitado riskColor) {
}
