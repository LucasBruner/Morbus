package br.com.morbus.agendamento.adapter.out.rabbitmq;

import java.time.LocalDateTime;
import java.util.UUID;

public interface IAgendamentoEventPublisher {

    void publishAppointmentConfirmed(UUID appointmentId,
                                     UUID slotId,
                                     UUID queueEntryId,
                                     UUID patientId,
                                     LocalDateTime agendadoEm);

    void publishAppointmentNoSlot(UUID queueEntryId,
                                  UUID patientId,
                                  UUID procedureId);
}
