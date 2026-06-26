package br.com.morbus.regulacao.ports.in;

import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.dto.CriarSolicitacaoCommand;

public interface ICriarSolicitacaoUseCase {
    Solicitacao execute(CriarSolicitacaoCommand command);
}
