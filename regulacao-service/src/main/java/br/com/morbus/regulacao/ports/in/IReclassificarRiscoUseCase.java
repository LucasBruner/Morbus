package br.com.morbus.regulacao.ports.in;

import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.dto.ReclassificarRiscoCommand;

public interface IReclassificarRiscoUseCase {
    Solicitacao execute(ReclassificarRiscoCommand command);
}
