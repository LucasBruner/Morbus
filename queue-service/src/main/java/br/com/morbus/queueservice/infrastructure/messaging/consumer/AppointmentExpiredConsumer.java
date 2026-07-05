package br.com.morbus.queueservice.infrastructure.messaging.consumer;

import br.com.morbus.queueservice.domain.usecase.ReinstatePatientInQueue;
import br.com.morbus.queueservice.infrastructure.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentExpiredConsumer {

    private final ReinstatePatientInQueue reinstatePatientInQueue;

    public AppointmentExpiredConsumer(ReinstatePatientInQueue reinstatePatientInQueue) {
        this.reinstatePatientInQueue = reinstatePatientInQueue;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_APPOINTMENT_EXPIRED)
    public void onAppointmentExpired(AppointmentExpiredEvent event) {
        reinstatePatientInQueue.execute(event.queueEntryId());
    }
}
