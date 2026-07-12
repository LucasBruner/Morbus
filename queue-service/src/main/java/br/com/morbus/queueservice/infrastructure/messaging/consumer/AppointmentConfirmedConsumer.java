package br.com.morbus.queueservice.infrastructure.messaging.consumer;

import br.com.morbus.queueservice.domain.usecase.ConfirmAppointment;
import br.com.morbus.queueservice.infrastructure.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentConfirmedConsumer {

    private final ConfirmAppointment confirmAppointment;

    public AppointmentConfirmedConsumer(ConfirmAppointment confirmAppointment) {
        this.confirmAppointment = confirmAppointment;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_APPOINTMENT_CONFIRMED)
    public void onAppointmentConfirmed(AppointmentConfirmedEvent event) {
        confirmAppointment.execute(event.queueEntryId());
    }
}
