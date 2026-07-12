package br.com.morbus.regulacao.ports.in;

import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.dto.ComplementarSolicitacaoCommand;

public interface IComplementarSolicitacaoUseCase {
    Solicitacao execute(ComplementarSolicitacaoCommand command);
}
