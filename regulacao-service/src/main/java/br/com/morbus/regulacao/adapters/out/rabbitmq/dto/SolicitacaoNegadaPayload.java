package br.com.morbus.regulacao.adapters.out.rabbitmq.dto;

import java.util.UUID;

public record SolicitacaoNegadaPayload(UUID solicitacaoId,
                                       UUID patientId,
                                       UUID procedureId,
                                       String justificativa) {
}
