package br.com.morbus.regulacao.domain.model;

import br.com.morbus.regulacao.domain.enums.EDecisaoRegulador;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Parecer {

    private UUID id;
    private UUID solicitacaoId;
    private UUID reguladorId;
    private EDecisaoRegulador decisao;
    private String justificativa;
    private LocalDateTime emitidoEm;

    public Parecer(UUID solicitacaoId, UUID reguladorId, EDecisaoRegulador decisao, String justificativa) {
        this.id = UUID.randomUUID();
        this.solicitacaoId = solicitacaoId;
        this.reguladorId = reguladorId;
        this.decisao = decisao;
        this.justificativa = justificativa;
        this.emitidoEm = LocalDateTime.now();
    }

    public Parecer(UUID id,
                   UUID solicitacaoId,
                   UUID reguladorId,
                   EDecisaoRegulador decisao,
                   String justificativa,
                   LocalDateTime emitidoEm) {
        this.id = id;
        this.solicitacaoId = solicitacaoId;
        this.reguladorId = reguladorId;
        this.decisao = decisao;
        this.justificativa = justificativa;
        this.emitidoEm = emitidoEm;
    }
}
