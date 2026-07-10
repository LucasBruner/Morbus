package br.com.morbus.agendamento.domain.port.in;

import br.com.morbus.agendamento.domain.model.AgendamentoComDetalhes;

import java.util.Optional;
import java.util.UUID;

public interface IDetalharAgendamentoUseCase {

    Optional<AgendamentoComDetalhes> execute(UUID id, UUID requesterId, String requesterRole);
}
