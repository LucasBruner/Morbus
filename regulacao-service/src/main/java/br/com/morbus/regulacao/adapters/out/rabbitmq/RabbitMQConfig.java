package br.com.morbus.regulacao.adapters.out.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String REGULACAO_EXCHANGE = "sus.regulacao.exchange";
    public static final String AGENDAMENTO_EXCHANGE = "sus.agendamento.exchange";
    public static final String REGULACAO_DLX = "sus.regulacao.dlx";

    public static final String RK_SOLICITACAO_APROVADA      = "solicitation.approved";
    public static final String RK_SOLICITACAO_NEGADA        = "solicitation.denied";
    public static final String RK_SOLICITACAO_DEVOLVIDA     = "solicitation.devolved";
    public static final String RK_SOLICITACAO_RECLASSIFICADA = "solicitation.reclassified";

    public static final String RK_APPOINTMENT_CREATED  = "appointment.created";
    public static final String RK_APPOINTMENT_ATTENDED = "appointment.attended";
    public static final String RK_APPOINTMENT_NO_SHOW  = "appointment.no_show";

    public static final String QUEUE_APPOINTMENT_CREATED  = "regulacao.appointment.created";
    public static final String QUEUE_APPOINTMENT_ATTENDED = "regulacao.appointment.attended";
    public static final String QUEUE_APPOINTMENT_NO_SHOW  = "regulacao.appointment.no_show";

    public static final String DLQ_APPOINTMENT_CREATED  = "regulacao.appointment.created.dlq";
    public static final String DLQ_APPOINTMENT_ATTENDED = "regulacao.appointment.attended.dlq";
    public static final String DLQ_APPOINTMENT_NO_SHOW  = "regulacao.appointment.no_show.dlq";

    @Bean
    public DirectExchange regulacaoExchange() {
        return new DirectExchange(REGULACAO_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange agendamentoExchange() {
        return new DirectExchange(AGENDAMENTO_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange regulacaoDlx() {
        return new DirectExchange(REGULACAO_DLX, true, false);
    }

    @Bean
    public Queue appointmentCreatedQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_CREATED)
                .withArgument("x-dead-letter-exchange", REGULACAO_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_APPOINTMENT_CREATED)
                .build();
    }

    @Bean
    public Queue appointmentAttendedQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_ATTENDED)
                .withArgument("x-dead-letter-exchange", REGULACAO_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_APPOINTMENT_ATTENDED)
                .build();
    }

    @Bean
    public Queue appointmentNoShowQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_NO_SHOW)
                .withArgument("x-dead-letter-exchange", REGULACAO_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_APPOINTMENT_NO_SHOW)
                .build();
    }

    @Bean
    public Queue appointmentCreatedDlq() {
        return QueueBuilder.durable(DLQ_APPOINTMENT_CREATED).build();
    }

    @Bean
    public Queue appointmentAttendedDlq() {
        return QueueBuilder.durable(DLQ_APPOINTMENT_ATTENDED).build();
    }

    @Bean
    public Queue appointmentNoShowDlq() {
        return QueueBuilder.durable(DLQ_APPOINTMENT_NO_SHOW).build();
    }

    @Bean
    public Binding bindingAppointmentCreated(Queue appointmentCreatedQueue,
                                              DirectExchange agendamentoExchange) {
        return BindingBuilder.bind(appointmentCreatedQueue)
                .to(agendamentoExchange)
                .with(RK_APPOINTMENT_CREATED);
    }

    @Bean
    public Binding bindingAppointmentAttended(Queue appointmentAttendedQueue,
                                               DirectExchange agendamentoExchange) {
        return BindingBuilder.bind(appointmentAttendedQueue)
                .to(agendamentoExchange)
                .with(RK_APPOINTMENT_ATTENDED);
    }

    @Bean
    public Binding bindingAppointmentNoShow(Queue appointmentNoShowQueue,
                                             DirectExchange agendamentoExchange) {
        return BindingBuilder.bind(appointmentNoShowQueue)
                .to(agendamentoExchange)
                .with(RK_APPOINTMENT_NO_SHOW);
    }

    @Bean
    public Binding bindingAppointmentCreatedDlq(Queue appointmentCreatedDlq,
                                                 DirectExchange regulacaoDlx) {
        return BindingBuilder.bind(appointmentCreatedDlq)
                .to(regulacaoDlx)
                .with(DLQ_APPOINTMENT_CREATED);
    }

    @Bean
    public Binding bindingAppointmentAttendedDlq(Queue appointmentAttendedDlq,
                                                  DirectExchange regulacaoDlx) {
        return BindingBuilder.bind(appointmentAttendedDlq)
                .to(regulacaoDlx)
                .with(DLQ_APPOINTMENT_ATTENDED);
    }

    @Bean
    public Binding bindingAppointmentNoShowDlq(Queue appointmentNoShowDlq,
                                                DirectExchange regulacaoDlx) {
        return BindingBuilder.bind(appointmentNoShowDlq)
                .to(regulacaoDlx)
                .with(DLQ_APPOINTMENT_NO_SHOW);
    }

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
        return template;
    }
}
