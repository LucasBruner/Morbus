package br.com.morbus.agendamento.adapter.in.rabbitmq;

import br.com.morbus.agendamento.adapter.out.rabbitmq.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PatientCalledConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PATIENT_CALLED)
    public void onPatientCalled(PatientCalledEvent event) {
        // TODO: acionar alocação de slot (AlocarSlotUseCase)
    }
}
