package br.com.morbus.agendamento.domain.port.in;

import br.com.morbus.agendamento.adapter.in.graphql.dto.AgendamentoDetalheRequestDTO;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

public interface IDetalharAgendamentoUseCase {

    Optional<AgendamentoDetalheRequestDTO> execute(UUID id, Authentication authentication);
}
