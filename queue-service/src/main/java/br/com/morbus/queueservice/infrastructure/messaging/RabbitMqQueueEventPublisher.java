package br.com.morbus.queueservice.infrastructure.messaging;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.infrastructure.messaging.DTO.QueueEventPayload;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Component
public class RabbitMqQueueEventPublisher implements IQueueEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public RabbitMqQueueEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishPatientRegistered(QueueEntry queueEntry) {
        QueueEventPayload payload = buildPayload("PATIENT_REGISTERED", queueEntry);
        rabbitTemplate.convertAndSend("sus.queue.exchange", "patient.registered", payload);
        log.info("Evento publicado: {}", payload.eventType());
    }

    @Override
    public void publishPatientCalled(QueueEntry queueEntry) {
        QueueEventPayload payload = buildPayload("PATIENT_CALLED", queueEntry);
        rabbitTemplate.convertAndSend("sus.queue.exchange", "patient.called", payload);
        log.info("Evento publicado: {}", payload.eventType());
    }

    @Override
    public void publishPriorityUpdated(QueueEntry queueEntry) {
        QueueEventPayload payload = buildPayload("PRIORITY_UPDATED", queueEntry);
        rabbitTemplate.convertAndSend("sus.queue.exchange", "priority.updated", payload);
        log.info("Evento publicado: {}", payload.eventType());
    }

    @Override
    public void publishPatientCancelled(QueueEntry queueEntry, String reason) {
        QueueEventPayload payload = buildPayload(queueEntry, reason);
        rabbitTemplate.convertAndSend("sus.queue.exchange", "patient.cancelled", payload);
        log.info("Evento publicado: {}", payload.eventType());
    }

    @Override
    public void publishPatientReinstated(QueueEntry queueEntry) {
        QueueEventPayload payload = buildPayload("PATIENT_REINSTATED", queueEntry);
        rabbitTemplate.convertAndSend("sus.queue.exchange", "patient.reinstated", payload);
        log.info("Evento publicado: {}", payload.eventType());
    }

    private QueueEventPayload buildPayload(String eventType, QueueEntry queueEntry) {
        String fullName = queueEntry.getPatient().getNome() + " " + queueEntry.getPatient().getSobrenome();
        return new QueueEventPayload(eventType,
                queueEntry.getId(),
                fullName,
                queueEntry.getPatient().getContato(),
                queueEntry.getProcedure().getNoProcedimento(),
                queueEntry.getRiskColor(),
                null,
                LocalDateTime.now());
    }

    private QueueEventPayload buildPayload(QueueEntry queueEntry, String reason) {
        String fullName = queueEntry.getPatient().getNome() + " " + queueEntry.getPatient().getSobrenome();
        return new QueueEventPayload("PATIENT_CANCELLED",
                queueEntry.getId(),
                fullName,
                queueEntry.getPatient().getContato(),
                queueEntry.getProcedure().getNoProcedimento(),
                queueEntry.getRiskColor(),
                reason,
                LocalDateTime.now());
    }
}
