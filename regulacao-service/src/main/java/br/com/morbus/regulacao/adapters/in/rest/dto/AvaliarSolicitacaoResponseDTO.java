package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.enums.EDecisaoRegulador;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.ports.in.dto.AvaliarSolicitacaoResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record AvaliarSolicitacaoResponseDTO(UUID parecerId,
                                            UUID solicitacaoId,
                                            UUID reguladorId,
                                            EDecisaoRegulador decisao,
                                            String justificativa,
                                            LocalDateTime emitidoEm,
                                            EStatusSolicitacao novoStatus) {

    public static AvaliarSolicitacaoResponseDTO fromResult(AvaliarSolicitacaoResult result) {
        var parecer = result.parecer();
        return new AvaliarSolicitacaoResponseDTO(
                parecer.getId(),
                parecer.getSolicitacaoId(),
                parecer.getReguladorId(),
                parecer.getDecisao(),
                parecer.getJustificativa(),
                parecer.getEmitidoEm(),
                result.novoStatus());
    }
}
