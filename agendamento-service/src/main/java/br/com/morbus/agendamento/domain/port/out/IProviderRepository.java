package br.com.morbus.agendamento.domain.port.out;

import br.com.morbus.agendamento.domain.model.Provider;

import java.util.Optional;
import java.util.UUID;

public interface IProviderRepository {

    Optional<Provider> findById(UUID id);
}
