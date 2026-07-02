package br.com.morbus.agendamento.adapter.out.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class AgendamentoEventPublisher implements IAgendamentoEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public AgendamentoEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishAppointmentConfirmed(UUID appointmentId,
                                            UUID slotId,
                                            UUID queueEntryId,
                                            UUID patientId,
                                            LocalDateTime agendadoEm) {
        AppointmentConfirmedEvent event = new AppointmentConfirmedEvent(
                appointmentId,
                slotId,
                queueEntryId,
                patientId,
                agendadoEm
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.AGENDAMENTO_EXCHANGE,
                RabbitMQConfig.RK_APPOINTMENT_CONFIRMED,
                event
        );
    }

    @Override
    public void publishAppointmentNoSlot(UUID queueEntryId,
                                         UUID patientId,
                                         UUID procedureId) {
        AppointmentNoSlotEvent event = new AppointmentNoSlotEvent(queueEntryId, patientId, procedureId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.AGENDAMENTO_EXCHANGE,
                RabbitMQConfig.RK_APPOINTMENT_NO_SLOT,
                event
        );
    }
}
