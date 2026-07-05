package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import org.springframework.stereotype.Repository;

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
        return toDomain(jpaRepository.save(toEntity(agendamento)));
    }

    @Override
    public Optional<Agendamento> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByPacienteIdAndSlotId(UUID pacienteId, UUID slotId) {
        return jpaRepository.existsByPacienteIdAndSlotId(pacienteId, slotId);
    }

    private AgendamentoEntity toEntity(Agendamento agendamento) {
        return new AgendamentoEntity(
                agendamento.getId(),
                agendamento.getQueueEntryId(),
                agendamento.getSlotId(),
                agendamento.getPacienteId(),
                agendamento.getStatus(),
                agendamento.getExpiresAt(),
                agendamento.getConfirmedAt(),
                agendamento.getAttendedAt(),
                agendamento.getCancellationReason(),
                agendamento.getCreatedAt(),
                agendamento.getUpdatedAt()
        );
    }

    private Agendamento toDomain(AgendamentoEntity entity) {
        return new Agendamento(
                entity.getId(),
                entity.getQueueEntryId(),
                entity.getSlotId(),
                entity.getPacienteId(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getConfirmedAt(),
                entity.getAttendedAt(),
                entity.getCancellationReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
