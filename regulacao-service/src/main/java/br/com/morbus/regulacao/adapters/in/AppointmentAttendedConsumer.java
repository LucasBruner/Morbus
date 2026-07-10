package br.com.morbus.regulacao.adapters.in;

import br.com.morbus.regulacao.adapters.out.rabbitmq.RabbitMQConfig;
import br.com.morbus.regulacao.ports.in.ITransicionarParaAtendidaUseCase;
import br.com.morbus.regulacao.ports.in.dto.AppointmentAttendedCommand;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AppointmentAttendedConsumer {

    private final ITransicionarParaAtendidaUseCase useCase;

    public AppointmentAttendedConsumer(ITransicionarParaAtendidaUseCase useCase) {
        this.useCase = useCase;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_APPOINTMENT_ATTENDED)
    @Transactional
    public void onAppointmentAttended(AppointmentAttendedEvent event) {
        useCase.execute(new AppointmentAttendedCommand(event.solicitacaoId(),
                event.appointmentId(),
                event.ocorridoEm()));
    }
}
