package br.com.morbus.agendamento.domain.port.out;

import br.com.morbus.agendamento.domain.model.HealthUnit;

import java.util.Optional;
import java.util.UUID;

public interface IHealthUnitRepository {

    Optional<HealthUnit> findById(UUID id);
}
