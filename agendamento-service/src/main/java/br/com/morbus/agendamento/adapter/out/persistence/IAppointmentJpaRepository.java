package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.enums.EAppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface IAppointmentJpaRepository extends JpaRepository<AppointmentEntity, UUID> {

    List<AppointmentEntity> findByStatusAndExpiresAtBefore(EAppointmentStatus status, LocalDateTime threshold);
}
