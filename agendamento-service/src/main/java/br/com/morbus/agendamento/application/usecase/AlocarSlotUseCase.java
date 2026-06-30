package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.application.command.AlocarSlotCommand;
import br.com.morbus.agendamento.domain.model.Appointment;
import br.com.morbus.agendamento.domain.port.in.IAlocarSlotUseCase;
import br.com.morbus.agendamento.domain.port.out.IAppointmentRepository;

import java.time.LocalDateTime;

public class AlocarSlotUseCase implements IAlocarSlotUseCase {

    private static final long EXPIRACAO_HORAS = 72;

    private final IAppointmentRepository appointmentRepository;

    public AlocarSlotUseCase(IAppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public Appointment execute(AlocarSlotCommand command) {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(EXPIRACAO_HORAS);

        Appointment appointment = new Appointment(
                command.queueEntryId(),
                command.slotId(),
                command.patientId(),
                expiresAt
        );

        return appointmentRepository.save(appointment);
    }
}
