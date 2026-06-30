package br.com.morbus.regulacao.adapters.out.rabbitmq.dto;

import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;

import java.util.UUID;

public record SolicitacaoAprovadaPayload(UUID solicitacaoId,
                                         UUID patientId,
                                         UUID procedureId,
                                         UUID unitExecutanteId,
                                         EDestino tipoFila,
                                         ERiscoSolicitado riskColor) {
}
