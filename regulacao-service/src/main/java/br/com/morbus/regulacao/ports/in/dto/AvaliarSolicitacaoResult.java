package br.com.morbus.regulacao.ports.in.dto;

import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Parecer;

public record AvaliarSolicitacaoResult(Parecer parecer, EStatusSolicitacao novoStatus) {
}
