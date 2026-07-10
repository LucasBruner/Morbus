package br.com.sus.notificationservice.consumer;

import br.com.sus.notificationservice.model.dto.AppointmentConfirmedEventDTO;
import br.com.sus.notificationservice.model.dto.AppointmentNoSlotEventDTO;
import br.com.sus.notificationservice.model.dto.AppointmentRescheduledEventDTO;
import br.com.sus.notificationservice.service.NotificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AppointmentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventConsumer.class);

    @Inject
    NotificationService notificationService;

    @Incoming("appointment-confirmed-events")
    public void consumeConfirmed(AppointmentConfirmedEventDTO event) {
        try {
            log.info("[CONSUMER] appointment.confirmed recebido: {}", event);
            notificationService.processAppointmentConfirmed(event);
        } catch (Exception e) {
            log.error("[CONSUMER] Erro ao processar appointment.confirmed", e);
        }
    }

    @Incoming("appointment-no-slot-events")
    public void consumeNoSlot(AppointmentNoSlotEventDTO event) {
        try {
            log.info("[CONSUMER] appointment.no_slot recebido: {}", event);
            notificationService.processAppointmentNoSlot(event);
        } catch (Exception e) {
            log.error("[CONSUMER] Erro ao processar appointment.no_slot", e);
        }
    }

    @Incoming("appointment-rescheduled-events")
    public void consumeRescheduled(AppointmentRescheduledEventDTO event) {
        try {
            log.info("[CONSUMER] appointment.rescheduled recebido: {}", event);
            notificationService.processAppointmentRescheduled(event);
        } catch (Exception e) {
            log.error("[CONSUMER] Erro ao processar appointment.rescheduled", e);
        }
    }
}
