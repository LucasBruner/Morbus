package br.com.morbus.regulacao.adapters.out.rabbitmq;

import br.com.morbus.regulacao.adapters.out.rabbitmq.dto.SolicitacaoAprovadaPayload;
import br.com.morbus.regulacao.adapters.out.rabbitmq.dto.SolicitacaoDevolvidaPayload;
import br.com.morbus.regulacao.adapters.out.rabbitmq.dto.SolicitacaoNegadaPayload;
import br.com.morbus.regulacao.adapters.out.rabbitmq.dto.SolicitacaoReclassificadaPayload;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.out.IRegulacaoEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static br.com.morbus.regulacao.adapters.out.rabbitmq.RabbitMQConfig.*;

@Slf4j
@Component
public class RabbitMqRegulacaoEventPublisher implements IRegulacaoEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqRegulacaoEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishSolicitacaoAprovada(Solicitacao solicitacao) {
        SolicitacaoAprovadaPayload payload = new SolicitacaoAprovadaPayload(
                solicitacao.getId(),
                solicitacao.getPatientId(),
                solicitacao.getProcedureId(),
                solicitacao.getUnidadeExecutanteId(),
                solicitacao.getDestino(),
                solicitacao.getRiskColor());

        rabbitTemplate.convertAndSend(REGULACAO_EXCHANGE, RK_SOLICITACAO_APROVADA, payload);
        log.info("Evento publicado: {}, id: {}, timestamp: {}", RK_SOLICITACAO_APROVADA, solicitacao.getId(), LocalDateTime.now());
    }

    @Override
    public void publishSolicitacaoNegada(Solicitacao solicitacao) {
        SolicitacaoNegadaPayload payload = new SolicitacaoNegadaPayload(
                solicitacao.getId(),
                solicitacao.getPatientId(),
                solicitacao.getProcedureId(),
                solicitacao.getJustificativaNegacao());

        rabbitTemplate.convertAndSend(REGULACAO_EXCHANGE, RK_SOLICITACAO_NEGADA, payload);
        log.info("Evento publicado: {}", RK_SOLICITACAO_NEGADA);
    }

    @Override
    public void publishSolicitacaoDevolvida(Solicitacao solicitacao) {
        SolicitacaoDevolvidaPayload payload = new SolicitacaoDevolvidaPayload(
                solicitacao.getId(),
                solicitacao.getPatientId(),
                solicitacao.getProcedureId(),
                solicitacao.getJustificativaNegacao());

        rabbitTemplate.convertAndSend(REGULACAO_EXCHANGE, RK_SOLICITACAO_DEVOLVIDA, payload);
        log.info("Evento publicado: {}", RK_SOLICITACAO_DEVOLVIDA);
    }

    @Override
    public void publishSolicitacaoReclassificada(Solicitacao solicitacao) {
        SolicitacaoReclassificadaPayload payload = new SolicitacaoReclassificadaPayload(
                solicitacao.getId(),
                solicitacao.getPatientId(),
                solicitacao.getProcedureId(),
                solicitacao.getRiskColor(),
                LocalDateTime.now());

        rabbitTemplate.convertAndSend(REGULACAO_EXCHANGE, RK_SOLICITACAO_RECLASSIFICADA, payload);
        log.info("Evento publicado: {}", RK_SOLICITACAO_RECLASSIFICADA);
    }
}
