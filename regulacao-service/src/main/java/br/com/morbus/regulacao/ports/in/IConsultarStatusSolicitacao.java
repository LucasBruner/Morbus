package br.com.morbus.regulacao.ports.in;

import br.com.morbus.regulacao.adapters.security.UserPrincipal;
import br.com.morbus.regulacao.domain.model.Solicitacao;

import java.util.UUID;

public interface IConsultarStatusSolicitacao {
    Solicitacao execute(UUID solicitacaoId, UserPrincipal principal);
}
