package br.com.sus.notificationservice.consumer;

import br.com.sus.notificationservice.model.dto.SolicitacaoDevolvidaEventDTO;
import br.com.sus.notificationservice.model.dto.SolicitacaoNegadaEventDTO;
import br.com.sus.notificationservice.service.NotificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SolicitacaoEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SolicitacaoEventConsumer.class);

    @Inject
    NotificationService notificationService;

    @Incoming("solicitation-denied-events")
    public void consumeNegada(SolicitacaoNegadaEventDTO event) {
        try {
            log.info("[CONSUMER] solicitation.denied recebido: {}", event);
            notificationService.processSolicitacaoNegada(event);
        } catch (Exception e) {
            log.error("[CONSUMER] Erro ao processar solicitation.denied", e);
        }
    }

    @Incoming("solicitation-devolved-events")
    public void consumeDevolvida(SolicitacaoDevolvidaEventDTO event) {
        try {
            log.info("[CONSUMER] solicitation.devolved recebido: {}", event);
            notificationService.processSolicitacaoDevolvida(event);
        } catch (Exception e) {
            log.error("[CONSUMER] Erro ao processar solicitation.devolved", e);
        }
    }
}
