package br.com.morbus.queueservice.infrastructure.messaging.consumer;

import br.com.morbus.queueservice.domain.enums.EDestino;
import br.com.morbus.queueservice.domain.enums.ERiskColor;

import java.util.UUID;

public record SolicitationApprovedEvent(UUID solicitacaoId,
                                        UUID patientId,
                                        UUID procedureId,
                                        UUID unitExecutanteId,
                                        EDestino tipoFila,
                                        ERiskColor riskColor) {
}
