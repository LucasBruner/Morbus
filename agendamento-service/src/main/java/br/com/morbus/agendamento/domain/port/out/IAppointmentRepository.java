package br.com.morbus.agendamento.domain.port.out;

import br.com.morbus.agendamento.domain.enums.EAppointmentStatus;
import br.com.morbus.agendamento.domain.model.Appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IAppointmentRepository {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(UUID id);

    List<Appointment> findByStatusAndExpiresAtBefore(EAppointmentStatus status, LocalDateTime threshold);
}
