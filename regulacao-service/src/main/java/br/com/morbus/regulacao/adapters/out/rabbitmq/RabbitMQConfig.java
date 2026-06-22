package br.com.morbus.regulacao.adapters.out.rabbitmq;

import org.springframework.amqp.core.*;
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

    public static final String RK_SOLICITACAO_APROVADA  = "solicitation.approved";
    public static final String RK_SOLICITACAO_NEGADA    = "solicitation.denied";
    public static final String RK_SOLICITACAO_DEVOLVIDA = "solicitation.devolved";

    public static final String RK_APPOINTMENT_CREATED  = "appointment.created";
    public static final String RK_APPOINTMENT_ATTENDED = "appointment.attended";
    public static final String RK_APPOINTMENT_NO_SHOW  = "appointment.no_show";

    public static final String QUEUE_SOLICITACAO_APROVADA  = "regulacao.solicitacao.aprovada";
    public static final String QUEUE_SOLICITACAO_NEGADA    = "regulacao.solicitacao.negada";
    public static final String QUEUE_SOLICITACAO_DEVOLVIDA = "regulacao.solicitacao.devolvida";

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
    public Queue solicitacaoAprovadaQueue() {
        return QueueBuilder.durable(QUEUE_SOLICITACAO_APROVADA).build();
    }

    @Bean
    public Queue solicitacaoNegadaQueue() {
        return QueueBuilder.durable(QUEUE_SOLICITACAO_NEGADA).build();
    }

    @Bean
    public Queue solicitacaoDevolvadaQueue() {
        return QueueBuilder.durable(QUEUE_SOLICITACAO_DEVOLVIDA).build();
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
    public Binding bindingSolicitacaoAprovada(Queue solicitacaoAprovadaQueue,
                                               DirectExchange regulacaoExchange) {
        return BindingBuilder.bind(solicitacaoAprovadaQueue)
                .to(regulacaoExchange)
                .with(RK_SOLICITACAO_APROVADA);
    }

    @Bean
    public Binding bindingSolicitacaoNegada(Queue solicitacaoNegadaQueue,
                                             DirectExchange regulacaoExchange) {
        return BindingBuilder.bind(solicitacaoNegadaQueue)
                .to(regulacaoExchange)
                .with(RK_SOLICITACAO_NEGADA);
    }

    @Bean
    public Binding bindingSolicitacaoDevolvida(Queue solicitacaoDevolvadaQueue,
                                                DirectExchange regulacaoExchange) {
        return BindingBuilder.bind(solicitacaoDevolvadaQueue)
                .to(regulacaoExchange)
                .with(RK_SOLICITACAO_DEVOLVIDA);
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
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
