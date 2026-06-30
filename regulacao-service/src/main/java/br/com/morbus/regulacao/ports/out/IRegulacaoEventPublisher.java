package br.com.morbus.regulacao.ports.out;

import br.com.morbus.regulacao.domain.model.Solicitacao;

public interface IRegulacaoEventPublisher {
    void publishSolicitacaoAprovada(Solicitacao solicitacao);
    void publishSolicitacaoNegada(Solicitacao solicitacao);
    void publishSolicitacaoDevolvida(Solicitacao solicitacao);
    void publishSolicitacaoReclassificada(Solicitacao solicitacao);
}
