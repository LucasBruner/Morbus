package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.model.Provider;
import br.com.morbus.agendamento.domain.port.out.IProviderRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ProviderPersistenceAdapter implements IProviderRepository {

    private final IProviderJpaRepository jpaRepository;

    public ProviderPersistenceAdapter(IProviderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Provider> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private Provider toDomain(ProviderEntity entity) {
        return new Provider(
                entity.getId(),
                entity.getNome(),
                entity.getCrm(),
                entity.getEspecialidade()
        );
    }
}
