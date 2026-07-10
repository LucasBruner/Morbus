package br.com.morbus.queueservice.infrastructure.messaging.consumer;

import br.com.morbus.queueservice.domain.usecase.ReinstatePatientInQueue;
import br.com.morbus.queueservice.infrastructure.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentNoSlotConsumer {

    private final ReinstatePatientInQueue reinstatePatientInQueue;

    public AppointmentNoSlotConsumer(ReinstatePatientInQueue reinstatePatientInQueue) {
        this.reinstatePatientInQueue = reinstatePatientInQueue;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_APPOINTMENT_NO_SLOT)
    public void onAppointmentNoSlot(AppointmentNoSlotEvent event) {
        reinstatePatientInQueue.execute(event.queueEntryId());
    }
}
