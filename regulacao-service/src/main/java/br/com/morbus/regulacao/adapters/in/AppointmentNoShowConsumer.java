package br.com.morbus.regulacao.adapters.in;

import br.com.morbus.regulacao.adapters.out.rabbitmq.RabbitMQConfig;
import br.com.morbus.regulacao.ports.in.ITransicionarParaFaltouUseCase;
import br.com.morbus.regulacao.ports.in.dto.AppointmentNoShowCommand;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentNoShowConsumer {

    private final ITransicionarParaFaltouUseCase useCase;

    public AppointmentNoShowConsumer(ITransicionarParaFaltouUseCase useCase) {
        this.useCase = useCase;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_APPOINTMENT_NO_SHOW)
    public void onAppointmentNoShow(AppointmentNoShowEvent event) {
        useCase.execute(new AppointmentNoShowCommand(event.solicitacaoId(),
                event.appointmentId(),
                event.ocorridoEm()));
    }
}
