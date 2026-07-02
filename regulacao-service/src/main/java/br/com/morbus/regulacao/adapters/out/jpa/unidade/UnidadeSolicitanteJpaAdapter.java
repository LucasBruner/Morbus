package br.com.morbus.regulacao.adapters.out.jpa.unidade;

import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;
import br.com.morbus.regulacao.ports.out.IUnidadeSolicitanteRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UnidadeSolicitanteJpaAdapter implements IUnidadeSolicitanteRepository {

    private final IUnidadeSolicitanteJpaRepository jpaRepository;

    public UnidadeSolicitanteJpaAdapter(IUnidadeSolicitanteJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public UnidadeSolicitante salvar(UnidadeSolicitante unidade) {
        UnidadeSolicitanteEntity entity = UnidadeSolicitanteEntity.fromDomain(unidade);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<UnidadeSolicitante> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(UnidadeSolicitanteEntity::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }
}