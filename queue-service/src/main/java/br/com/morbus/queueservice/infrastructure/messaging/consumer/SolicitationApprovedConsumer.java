package br.com.morbus.queueservice.infrastructure.messaging.consumer;

import br.com.morbus.queueservice.domain.usecase.AddToQueue;
import br.com.morbus.queueservice.infrastructure.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SolicitationApprovedConsumer {

    private final AddToQueue addToQueue;

    public SolicitationApprovedConsumer(AddToQueue addToQueue) {
        this.addToQueue = addToQueue;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SOLICITATION_APPROVED)
    public void onSolicitationApproved(SolicitationApprovedEvent event) {
        addToQueue.execute(event.patientId(), event.procedureId(), event.tipoFila(), event.riskColor(),
                event.solicitacaoId(), event.unitExecutanteId());
    }
}
