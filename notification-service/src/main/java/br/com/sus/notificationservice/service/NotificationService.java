package br.com.sus.notificationservice.service;

import br.com.sus.notificationservice.model.Notification;
import br.com.sus.notificationservice.model.dto.AppointmentCancelledEventDTO;
import br.com.sus.notificationservice.model.dto.AppointmentConfirmedEventDTO;
import br.com.sus.notificationservice.model.dto.AppointmentExpiredEventDTO;
import br.com.sus.notificationservice.model.dto.AppointmentNoSlotEventDTO;
import br.com.sus.notificationservice.model.dto.AppointmentRescheduledEventDTO;
import br.com.sus.notificationservice.model.dto.QueueEventDTO;
import br.com.sus.notificationservice.model.dto.SolicitacaoDevolvidaEventDTO;
import br.com.sus.notificationservice.model.dto.SolicitacaoNegadaEventDTO;
import br.com.sus.notificationservice.model.enums.ENotificationType;
import br.com.sus.notificationservice.repository.NotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.logging.Logger;

@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());

    @Inject
    NotificationRepository notificationRepository;

    @Inject
    EmailService emailService;

    @Transactional
    public void process(QueueEventDTO event) {
        String message = buildMessage(event);
        if (message == null) return;

        persist(event.eventType(), event.patientName(), event.patientContact(), message, event.timestamp());
    }

    @Transactional
    public void processSolicitacaoNegada(SolicitacaoNegadaEventDTO event) {
        String message = "Sua solicitação de atendimento foi negada. Motivo: %s".formatted(event.justificativa());
        persist(ENotificationType.SOLICITATION_DENIED.name(), null, null, message, event.negadoEm());
    }

    @Transactional
    public void processSolicitacaoDevolvida(SolicitacaoDevolvidaEventDTO event) {
        String message = "Sua solicitação foi devolvida para complementação de informações. Motivo: %s".formatted(event.justificativa());
        persist(ENotificationType.SOLICITATION_DEVOLVED.name(), null, null, message, LocalDateTime.now());
    }

    @Transactional
    public void processAppointmentConfirmed(AppointmentConfirmedEventDTO event) {
        String message = "Seu agendamento foi confirmado para %s.".formatted(event.agendadoEm());
        persist(ENotificationType.APPOINTMENT_CONFIRMED.name(), null, null, message, event.agendadoEm());
    }

    @Transactional
    public void processAppointmentNoSlot(AppointmentNoSlotEventDTO event) {
        String message = "Não há vaga disponível no momento para o seu atendimento. Você será notificado assim que uma vaga for aberta.";
        persist(ENotificationType.APPOINTMENT_NO_SLOT.name(), null, null, message, LocalDateTime.now());
    }

    @Transactional
    public void processAppointmentRescheduled(AppointmentRescheduledEventDTO event) {
        String message = "Seu agendamento foi reagendado para %s.".formatted(event.reagendadoEm());
        persist(ENotificationType.APPOINTMENT_RESCHEDULED.name(), null, null, message, event.reagendadoEm());
    }

    @Transactional
    public void processAppointmentCancelled(AppointmentCancelledEventDTO event) {
        String message = "Seu agendamento foi cancelado. Motivo: %s".formatted(event.motivo());
        persist(ENotificationType.APPOINTMENT_CANCELLED.name(), null, null, message, event.canceladoEm());
    }

    @Transactional
    public void processAppointmentExpired(AppointmentExpiredEventDTO event) {
        String message = "Seu agendamento expirou por falta de confirmação em %s.".formatted(event.expirouEm());
        persist(ENotificationType.APPOINTMENT_EXPIRED.name(), null, null, message, event.expirouEm());
    }

    private void persist(String eventType, String recipientName, String recipientContact, String message, LocalDateTime sentAt) {
        Notification notification = new Notification();
        notification.eventType = eventType;
        notification.recipientName = recipientName;
        notification.recipientContact = recipientContact;
        notification.message = message;
        notification.sentAt = sentAt;
        notification.status = "ENVIADO";

        notificationRepository.persist(notification);
        try {
            emailService.send(notification.recipientContact, "Morbus Notification", notification.message);
        } catch (Exception e) {
            notification.status = "FALHA";
        }
    }

    private String buildMessage(QueueEventDTO eventDTO) {
        ENotificationType type;
        try {
            type = ENotificationType.valueOf(eventDTO.eventType());
        } catch (IllegalArgumentException e) {
            LOG.warning("Tipo de evento desconhecido: " + eventDTO.eventType());
            return null;
        }
        return switch (type){
            case PATIENT_REGISTERED -> "Você foi cadastrado na fila para %s. Sua classificação é %s."
                    .formatted(eventDTO.procedureName(), eventDTO.riskColor());
            case PATIENT_CALLED -> "É a sua vez! Compareça ao guichê para %s."
                    .formatted(eventDTO.procedureName());
            case PRIORITY_UPDATED -> "Sua prioridade na fila foi atualizada para %s."
                    .formatted(eventDTO.riskColor());
            case PATIENT_CANCELLED -> "Seu agendamento para %s foi cancelado."
                    .formatted(eventDTO.procedureName());
            case PATIENT_REINSTATED -> "Você foi reincluído na fila para %s."
                    .formatted(eventDTO.procedureName());
            case SOLICITATION_DENIED, SOLICITATION_DEVOLVED, APPOINTMENT_CONFIRMED, APPOINTMENT_NO_SLOT,
                 APPOINTMENT_RESCHEDULED, APPOINTMENT_CANCELLED, APPOINTMENT_EXPIRED -> {
                LOG.warning("Tipo de evento nao esperado no canal queue-events: " + type);
                yield null;
            }
        };
    }
}
