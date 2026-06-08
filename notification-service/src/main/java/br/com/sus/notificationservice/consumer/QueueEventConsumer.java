package br.com.sus.notificationservice.consumer;

import br.com.sus.notificationservice.model.dto.QueueEventDTO;
import br.com.sus.notificationservice.service.NotificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class QueueEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(QueueEventConsumer.class);

    @Inject
    NotificationService notificationService;

    @Incoming("queue-events")
    public void consume(QueueEventDTO event) {
        try {
            log.info("[CONSUMER] Evento recebido: {} para {}", event, event.patientName());
            notificationService.process(event);
            log.info("[CONSUMER] Evento processado com sucesso");
        } catch (Exception e) {
            log.error("[CONSUMER] Erro ao processar evento", e);
        }
    }
}
