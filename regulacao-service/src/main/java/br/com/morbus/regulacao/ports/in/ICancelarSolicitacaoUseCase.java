package br.com.morbus.regulacao.ports.in;

import java.util.UUID;

public interface ICancelarSolicitacaoUseCase {
    void execute(UUID idSolicitacao);
}
