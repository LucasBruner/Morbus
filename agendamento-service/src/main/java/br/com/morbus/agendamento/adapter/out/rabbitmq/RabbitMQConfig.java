package br.com.morbus.agendamento.adapter.out.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

    public static final String AGENDAMENTO_EXCHANGE = "sus.agendamento.exchange";
    public static final String AGENDAMENTO_DLX = "sus.agendamento.dlx";

    public static final String SUS_QUEUE_EXCHANGE = "sus.queue.exchange";
    public static final String QUEUE_PATIENT_REINSTATED = "agendamento.patient.reinstated";
    public static final String RK_PATIENT_REINSTATED = "agendamento.patient.reinstated";

    public static final String QUEUE_SOLICITACAO_APROVADA  = "regulacao.solicitacao.aprovada";
    public static final String QUEUE_SOLICITACAO_NEGADA    = "regulacao.solicitacao.negada";
    public static final String QUEUE_SOLICITACAO_DEVOLVIDA = "regulacao.solicitacao.devolvida";

    public static final String QUEUE_APPOINTMENT_CREATED  = "agendamento.appointment.created";
    public static final String QUEUE_APPOINTMENT_ATTENDED = "agendamento.appointment.attended";
    public static final String QUEUE_APPOINTMENT_NO_SHOW  = "agendamento.appointment.no_show";
    public static final String QUEUE_APPOINTMENT_EXPIRED  = "agendamento.appointment.expired";

    public static final String DLQ_APPOINTMENT_CREATED  = "agendamento.appointment.created.dlq";
    public static final String DLQ_APPOINTMENT_ATTENDED = "agendamento.appointment.attended.dlq";
    public static final String DLQ_APPOINTMENT_NO_SHOW  = "agendamento.appointment.no_show.dlq";
    public static final String DLQ_APPOINTMENT_EXPIRED  = "agendamento.appointment.expired.dlq";

    public static final String RK_SOLICITACAO_APROVADA  = "solicitation.approved";
    public static final String RK_SOLICITACAO_NEGADA    = "solicitation.denied";
    public static final String RK_SOLICITACAO_DEVOLVIDA = "solicitation.devolved";

    public static final String RK_APPOINTMENT_CREATED  = "appointment.created";
    public static final String RK_APPOINTMENT_ATTENDED = "appointment.attended";
    public static final String RK_APPOINTMENT_NO_SHOW  = "appointment.no_show";
    public static final String RK_APPOINTMENT_EXPIRED  = "appointment.expired";

    @Bean
    public DirectExchange agendamentoExchange() {
        return new DirectExchange(AGENDAMENTO_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange agendamentoDlx() {
        return new DirectExchange(AGENDAMENTO_DLX, true, false);
    }

    @Bean
    public TopicExchange susQueueExchange() {
        return new TopicExchange(SUS_QUEUE_EXCHANGE, true, false);
    }

    @Bean
    public Queue patientReinstatedQueue() {
        return QueueBuilder.durable(QUEUE_PATIENT_REINSTATED).build();
    }

    @Bean
    public Binding patientReinstatedBinding() {
        return BindingBuilder
                .bind(patientReinstatedQueue())
                .to(susQueueExchange())
                .with(RK_PATIENT_REINSTATED);
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
                .withArgument("x-dead-letter-exchange", AGENDAMENTO_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_APPOINTMENT_CREATED)
                .build();
    }

    @Bean
    public Queue appointmentAttendedQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_ATTENDED)
                .withArgument("x-dead-letter-exchange", AGENDAMENTO_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_APPOINTMENT_ATTENDED)
                .build();
    }

    @Bean
    public Queue appointmentNoShowQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_NO_SHOW)
                .withArgument("x-dead-letter-exchange", AGENDAMENTO_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_APPOINTMENT_NO_SHOW)
                .build();
    }

    @Bean
    public Queue appointmentExpiredQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_EXPIRED)
                .withArgument("x-dead-letter-exchange", AGENDAMENTO_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_APPOINTMENT_EXPIRED)
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
    public Binding bindingAppointmentExpired(Queue appointmentNoShowQueue,
                                            DirectExchange agendamentoExchange) {
        return BindingBuilder.bind(appointmentNoShowQueue)
                .to(agendamentoExchange)
                .with(RK_APPOINTMENT_EXPIRED);
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
    public Binding bindingAppointmentExpiredDlq(Queue appointmentNoShowDlq,
                                               DirectExchange regulacaoDlx) {
        return BindingBuilder.bind(appointmentNoShowDlq)
                .to(regulacaoDlx)
                .with(DLQ_APPOINTMENT_EXPIRED);
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
