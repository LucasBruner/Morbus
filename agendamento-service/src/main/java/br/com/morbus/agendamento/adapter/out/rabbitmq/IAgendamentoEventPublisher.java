package br.com.morbus.agendamento.adapter.out.rabbitmq;

import java.time.LocalDateTime;
import java.util.UUID;

public interface IAgendamentoEventPublisher {

    void publishAppointmentConfirmed(UUID appointmentId,
                                     UUID slotId,
                                     UUID queueEntryId,
                                     UUID patientId,
                                     LocalDateTime agendadoEm);

    void publishAppointmentCreated(UUID solicitacaoId,
                                   UUID appointmentId,
                                   UUID slotId);

    void publishAppointmentNoSlot(UUID queueEntryId,
                                  UUID patientId,
                                  UUID procedureId);

    void publishAppointmentAttended(UUID appointmentId,
                                    UUID queueEntryId,
                                    UUID patientId,
                                    LocalDateTime ocorridoEm);

    void publishPatientNoShow(UUID appointmentId,
                              UUID queueEntryId,
                              UUID patientId,
                              LocalDateTime ocorridoEm);

    void publishAppointmentExpired(UUID appointmentId,
                                   UUID queueEntryId,
                                   UUID patientId,
                                   LocalDateTime expirouEm);

    void publishAppointmentRescheduled(UUID appointmentId,
                                       UUID slotId,
                                       UUID queueEntryId,
                                       UUID patientId,
                                       LocalDateTime reagendadoEm);

    void publishAppointmentCancelled(UUID appointmentId,
                                     UUID queueEntryId,
                                     UUID patientId,
                                     String motivo,
                                     LocalDateTime canceladoEm);
}
