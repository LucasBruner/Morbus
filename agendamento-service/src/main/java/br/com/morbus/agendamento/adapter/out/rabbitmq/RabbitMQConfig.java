package br.com.morbus.agendamento.adapter.out.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

    // Exchange publicado pelo agendamento-service
    public static final String AGENDAMENTO_EXCHANGE = "sus.agendamento.exchange";
    public static final String AGENDAMENTO_DLX      = "sus.agendamento.dlx";

    // Exchange publicado pelo queue-service (agendamento-service consome)
    public static final String SUS_QUEUE_EXCHANGE = "sus.queue.exchange";

    // Routing keys publicadas pelo agendamento-service
    public static final String RK_APPOINTMENT_CREATED     = "appointment.created";
    public static final String RK_APPOINTMENT_CONFIRMED   = "appointment.confirmed";
    public static final String RK_APPOINTMENT_CANCELLED   = "appointment.cancelled";
    public static final String RK_APPOINTMENT_RESCHEDULED = "appointment.rescheduled";
    public static final String RK_APPOINTMENT_ATTENDED    = "appointment.attended";
    public static final String RK_APPOINTMENT_NO_SLOT     = "appointment.no_slot";
    public static final String RK_APPOINTMENT_EXPIRED     = "appointment.expired";
    public static final String RK_PATIENT_NO_SHOW         = "patient.no_show";

    // Fila consumida pelo agendamento-service ← sus.queue.exchange
    public static final String QUEUE_PATIENT_CALLED = "queue.patient.called";
    public static final String RK_PATIENT_CALLED    = "patient.called";
    public static final String DLQ_PATIENT_CALLED   = "queue.patient.called.dlq";

    // ─── Exchanges ────────────────────────────────────────────────────────────

    @Bean
    public DirectExchange agendamentoExchange() {
        return new DirectExchange(AGENDAMENTO_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange agendamentoDlx() {
        return new DirectExchange(AGENDAMENTO_DLX, true, false);
    }

    @Bean
    public DirectExchange susQueueExchange() {
        return new DirectExchange(SUS_QUEUE_EXCHANGE, true, false);
    }

    // ─── Fila consumida (sus.queue.exchange) ─────────────────────────────────

    @Bean
    public Queue patientCalledQueue() {
        return QueueBuilder.durable(QUEUE_PATIENT_CALLED)
                .withArgument("x-dead-letter-exchange", AGENDAMENTO_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_PATIENT_CALLED)
                .build();
    }

    @Bean
    public Queue patientCalledDlq() {
        return QueueBuilder.durable(DLQ_PATIENT_CALLED).build();
    }

    @Bean
    public Binding bindingPatientCalled(Queue patientCalledQueue,
                                        DirectExchange susQueueExchange) {
        return BindingBuilder.bind(patientCalledQueue)
                .to(susQueueExchange)
                .with(RK_PATIENT_CALLED);
    }

    @Bean
    public Binding bindingPatientCalledDlq(Queue patientCalledDlq,
                                           DirectExchange agendamentoDlx) {
        return BindingBuilder.bind(patientCalledDlq)
                .to(agendamentoDlx)
                .with(DLQ_PATIENT_CALLED);
    }

    // ─── Infraestrutura de mensageria ─────────────────────────────────────────

    @Bean
    public JsonMapper objectMapper() {
        return JsonMapper.builder().build();
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter(JsonMapper objectMapper) {
        return new JacksonJsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         JacksonJsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setChannelTransacted(true);
        return template;
    }
}
