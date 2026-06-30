package br.com.morbus.regulacao.adapters.out.jpa.parecer;

import br.com.morbus.regulacao.domain.model.Parecer;
import br.com.morbus.regulacao.ports.out.IParecerRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ParecerJpaAdapter implements IParecerRepository {

    private final IParecerJpaRepository jpaRepository;

    public ParecerJpaAdapter(IParecerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Parecer save(Parecer parecer) {
        ParecerEntity entity = ParecerEntity.fromDomain(parecer);
        return jpaRepository.save(entity).toDomain();
    }
}
