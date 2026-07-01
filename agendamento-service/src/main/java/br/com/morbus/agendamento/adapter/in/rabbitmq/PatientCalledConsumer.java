package br.com.morbus.agendamento.adapter.in.rabbitmq;

import br.com.morbus.agendamento.adapter.out.rabbitmq.RabbitMQConfig;
import br.com.morbus.agendamento.domain.port.in.IAlocarPacienteEmSlotUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PatientCalledConsumer {

    private final IAlocarPacienteEmSlotUseCase alocarPacienteEmSlotUseCase;

    public PatientCalledConsumer(IAlocarPacienteEmSlotUseCase alocarPacienteEmSlotUseCase) {
        this.alocarPacienteEmSlotUseCase = alocarPacienteEmSlotUseCase;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PATIENT_CALLED)
    public void onPatientCalled(PatientCalledEvent event) {
        alocarPacienteEmSlotUseCase.execute(event);
    }
}
