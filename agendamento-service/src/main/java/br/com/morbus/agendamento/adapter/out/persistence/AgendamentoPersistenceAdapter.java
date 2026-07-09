package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.model.AgendamentoComDetalhes;
import br.com.morbus.agendamento.domain.model.HealthUnit;
import br.com.morbus.agendamento.domain.model.Provider;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;
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

    @Override
    public List<Agendamento> findAllByStatusAndExpiresAtBefore(EStatusAgendamento status, LocalDateTime now) {
        return jpaRepository.findAllByStatusAndExpiresAtBefore(status, now)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<AgendamentoComDetalhes> findByPatientAndStatusAndDate(UUID patientId,
                                                                      UUID unitId,
                                                                      EStatusAgendamento status,
                                                                      LocalDateTime dateFrom,
                                                                      LocalDateTime dateTo) {
        return jpaRepository.findByPatientAndStatusAndDate(patientId, unitId, status, dateFrom, dateTo)
                .stream()
                .map(this::toDomain)
                .toList();
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
                agendamento.getNoShowAt(),
                agendamento.getCancellationReason(),
                agendamento.getCreatedAt(),
                agendamento.getUpdatedAt()
        );
    }

    private Agendamento toDomain(AgendamentoEntity entity) {
        return Agendamento.fromPersistence(new Agendamento.AgendamentoSnapshot(
                entity.getId(),
                entity.getQueueEntryId(),
                entity.getSlotId(),
                entity.getPacienteId(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getConfirmedAt(),
                entity.getAttendedAt(),
                entity.getNoShowAt(),
                entity.getCancellationReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        ));
    }

    private AgendamentoComDetalhes toDomain(AgendamentoListProjection projection) {
        return new AgendamentoComDetalhes(
                toDomain(projection.agendamento()),
                toDomain(projection.slot()),
                toDomain(projection.schedule()),
                toDomain(projection.unit()),
                projection.provider() != null ? toDomain(projection.provider()) : null
        );
    }

    private Slot toDomain(SlotEntity entity) {
        return new Slot(
                entity.getId(),
                entity.getScheduleId(),
                entity.getDataHora(),
                entity.getCapacidade(),
                entity.getReservados(),
                entity.getStatus()
        );
    }

    private Schedule toDomain(ScheduleEntity entity) {
        return new Schedule(
                entity.getId(),
                entity.getUnitId(),
                entity.getProviderId(),
                entity.getProcedureId(),
                entity.getDiaDaSemana(),
                entity.getHorarioInicio(),
                entity.getHorarioFim(),
                entity.getSlotDuracaoMinutos(),
                entity.getCapacidade(),
                entity.isAtivo()
        );
    }

    private HealthUnit toDomain(HealthUnitEntity entity) {
        return new HealthUnit(
                entity.getId(),
                entity.getNome(),
                entity.getCnes(),
                entity.getMunicipio(),
                entity.getUf()
        );
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
