package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.model.HealthUnit;
import br.com.morbus.agendamento.domain.port.out.IHealthUnitRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class HealthUnitPersistenceAdapter implements IHealthUnitRepository {

    private final IHealthUnitJpaRepository jpaRepository;

    public HealthUnitPersistenceAdapter(IHealthUnitJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<HealthUnit> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private HealthUnit toDomain(HealthUnitEntity entity) {
        return new HealthUnit(
                entity.getId(),
                entity.getNome(),
                entity.getCnes(),
                entity.getMunicipio(),
                entity.getUf(),
                entity.getEndereco(),
                entity.getTelefone()
        );
    }
}
