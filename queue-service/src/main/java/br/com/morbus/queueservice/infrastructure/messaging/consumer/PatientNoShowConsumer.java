package br.com.morbus.queueservice.infrastructure.messaging.consumer;

import br.com.morbus.queueservice.domain.usecase.ReinstatePatientInQueue;
import br.com.morbus.queueservice.infrastructure.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PatientNoShowConsumer {

    private final ReinstatePatientInQueue reinstatePatientInQueue;

    public PatientNoShowConsumer(ReinstatePatientInQueue reinstatePatientInQueue) {
        this.reinstatePatientInQueue = reinstatePatientInQueue;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PATIENT_NO_SHOW)
    public void onPatientNoShow(PatientNoShowEvent event) {
        reinstatePatientInQueue.execute(event.queueEntryId());
    }
}
