package br.com.morbus.regulacao.ports.in;

import br.com.morbus.regulacao.ports.in.dto.AvaliarSolicitacaoCommand;
import br.com.morbus.regulacao.ports.in.dto.AvaliarSolicitacaoResult;

public interface IAvaliarSolicitacaoUseCase {
    AvaliarSolicitacaoResult execute(AvaliarSolicitacaoCommand command);
}
