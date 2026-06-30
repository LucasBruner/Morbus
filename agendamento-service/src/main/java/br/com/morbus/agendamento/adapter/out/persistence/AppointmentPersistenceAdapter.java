package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.enums.EAppointmentStatus;
import br.com.morbus.agendamento.domain.model.Appointment;
import br.com.morbus.agendamento.domain.port.out.IAppointmentRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AppointmentPersistenceAdapter implements IAppointmentRepository {

    private final IAppointmentJpaRepository jpaRepository;

    public AppointmentPersistenceAdapter(IAppointmentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Appointment save(Appointment appointment) {
        AppointmentEntity entity = AppointmentEntity.fromDomain(appointment);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Appointment> findById(UUID id) {
        return jpaRepository.findById(id).map(AppointmentEntity::toDomain);
    }

    @Override
    public List<Appointment> findByStatusAndExpiresAtBefore(EAppointmentStatus status, LocalDateTime threshold) {
        return jpaRepository.findByStatusAndExpiresAtBefore(status, threshold)
                .stream()
                .map(AppointmentEntity::toDomain)
                .toList();
    }
}
