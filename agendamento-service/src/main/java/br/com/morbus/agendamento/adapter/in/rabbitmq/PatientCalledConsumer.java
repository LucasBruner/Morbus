package br.com.morbus.agendamento.adapter.in.rabbitmq;

import br.com.morbus.agendamento.adapter.out.rabbitmq.RabbitMQConfig;
import br.com.morbus.agendamento.application.command.CriarAgendamentoCommand;
import br.com.morbus.agendamento.domain.port.in.ICriarAgendamentoUseCase;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PatientCalledConsumer {

    private final ICriarAgendamentoUseCase criarAgendamentoUseCase;

    public PatientCalledConsumer(ICriarAgendamentoUseCase criarAgendamentoUseCase) {
        this.criarAgendamentoUseCase = criarAgendamentoUseCase;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PATIENT_CALLED)
    public void onPatientCalled(PatientCalledEvent event) {
        // Prazo de 72h para confirmação
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(72);

        criarAgendamentoUseCase.execute(new CriarAgendamentoCommand(
                event.queueEntryId(),
                event.slotId(),
                event.pacienteId(),
                expiresAt
        ));
    }
}
