package br.com.morbus.regulacao.adapters.out.rabbitmq.dto;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;

import java.time.LocalDateTime;
import java.util.UUID;

public record SolicitacaoReclassificadaPayload(UUID solicitacaoId,
                                               UUID patientId,
                                               UUID procedureId,
                                               ERiscoSolicitado novoRiskColor,
                                               LocalDateTime reclassificadoEm) {
}
