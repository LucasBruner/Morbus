package br.com.morbus.queueservice.infrastructure.messaging.consumer;

import br.com.morbus.queueservice.domain.usecase.ReinstatePatientInQueue;
import br.com.morbus.queueservice.infrastructure.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentCancelledConsumer {

    private final ReinstatePatientInQueue reinstatePatientInQueue;

    public AppointmentCancelledConsumer(ReinstatePatientInQueue reinstatePatientInQueue) {
        this.reinstatePatientInQueue = reinstatePatientInQueue;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_APPOINTMENT_CANCELLED)
    public void onAppointmentCancelled(AppointmentCancelledEvent event) {
        reinstatePatientInQueue.execute(event.queueEntryId());
    }
}
