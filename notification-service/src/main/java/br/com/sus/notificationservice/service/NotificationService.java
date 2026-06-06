package br.com.sus.notificationservice.service;

import br.com.sus.notificationservice.model.Notification;
import br.com.sus.notificationservice.model.dto.QueueEventDTO;
import br.com.sus.notificationservice.model.enums.ENotificationType;
import br.com.sus.notificationservice.repository.NotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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

        Notification notification = new Notification();
        notification.eventType = event.eventType();
        notification.recipientName = event.patientName();
        notification.recipientContact = event.patientContact();
        notification.message = message;
        notification.sentAt = event.timestamp();
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
        };
    }
}
