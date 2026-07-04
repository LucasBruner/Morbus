package br.com.morbus.regulacao.adapters.in;

import br.com.morbus.regulacao.adapters.out.rabbitmq.RabbitMQConfig;
import br.com.morbus.regulacao.ports.in.ITransicionarParaAgendadaUseCase;
import br.com.morbus.regulacao.ports.in.dto.AppointmentCreatedCommand;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentCreatedConsumer {

    private final ITransicionarParaAgendadaUseCase useCase;

    public AppointmentCreatedConsumer(ITransicionarParaAgendadaUseCase useCase) {
        this.useCase = useCase;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_APPOINTMENT_CREATED)
    public void onAppointmentCreated(AppointmentCreatedEvent event) {
        useCase.execute(new AppointmentCreatedCommand(event.solicitacaoId(),
                event.appointmentId(),
                event.slotId()));
    }
}
