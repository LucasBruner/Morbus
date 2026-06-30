package br.com.morbus.regulacao.ports.in.dto;

import br.com.morbus.regulacao.domain.enums.EDecisaoRegulador;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;

import java.util.UUID;

public record AvaliarSolicitacaoCommand(UUID solicitacaoId,
                                        UUID reguladorId,
                                        EDecisaoRegulador decisao,
                                        ERiscoSolicitado riskColorDefinido,
                                        String justificativa,
                                        UUID unidadeExecutanteId) {
}
