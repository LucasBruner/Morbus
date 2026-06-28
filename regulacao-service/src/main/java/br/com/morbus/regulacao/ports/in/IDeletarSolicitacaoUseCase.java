package br.com.morbus.regulacao.ports.in;

import java.util.UUID;

public interface IDeletarSolicitacaoUseCase {
    void execute(UUID idSolicitacao);
}
