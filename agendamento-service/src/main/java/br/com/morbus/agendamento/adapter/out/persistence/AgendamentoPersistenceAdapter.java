package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AgendamentoPersistenceAdapter implements IAgendamentoRepository {

    private final IAgendamentoJpaRepository jpaRepository;

    public AgendamentoPersistenceAdapter(IAgendamentoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Agendamento save(Agendamento agendamento) {
        AgendamentoEntity entity = AgendamentoEntity.fromDomain(agendamento);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Agendamento> findById(UUID id) {
        return jpaRepository.findById(id).map(AgendamentoEntity::toDomain);
    }

    @Override
    public List<Agendamento> findByStatusAndExpiresAtBefore(EStatusAgendamento status, LocalDateTime threshold) {
        return jpaRepository.findByStatusAndExpiresAtBefore(status, threshold)
                .stream()
                .map(AgendamentoEntity::toDomain)
                .toList();
    }
}
