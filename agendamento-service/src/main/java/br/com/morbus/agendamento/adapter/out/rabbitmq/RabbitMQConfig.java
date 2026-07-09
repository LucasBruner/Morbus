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

    // Filas publicadas pelo agendamento-service → sus.agendamento.exchange
    public static final String QUEUE_APPOINTMENT_CONFIRMED   = "queue.appointment.confirmed";
    public static final String QUEUE_APPOINTMENT_CANCELLED   = "queue.appointment.cancelled";
    public static final String QUEUE_APPOINTMENT_RESCHEDULED = "queue.appointment.rescheduled";
    public static final String QUEUE_APPOINTMENT_ATTENDED    = "queue.appointment.attended";
    public static final String QUEUE_APPOINTMENT_NO_SLOT     = "queue.appointment.no_slot";
    public static final String QUEUE_APPOINTMENT_EXPIRED     = "queue.appointment.expired";
    public static final String QUEUE_PATIENT_NO_SHOW         = "queue.patient.no_show";

    // Routing keys publicadas pelo agendamento-service
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

    // ─── Filas publicadas (sus.agendamento.exchange) ──────────────────────────

    @Bean
    public Queue appointmentConfirmedQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_CONFIRMED).build();
    }

    @Bean
    public Queue appointmentCancelledQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_CANCELLED).build();
    }

    @Bean
    public Queue appointmentRescheduledQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_RESCHEDULED).build();
    }

    @Bean
    public Queue appointmentAttendedQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_ATTENDED).build();
    }

    @Bean
    public Queue appointmentNoSlotQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_NO_SLOT).build();
    }

    @Bean
    public Queue appointmentExpiredQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_EXPIRED).build();
    }

    @Bean
    public Queue patientNoShowQueue() {
        return QueueBuilder.durable(QUEUE_PATIENT_NO_SHOW).build();
    }

    @Bean
    public Binding bindingAppointmentConfirmed(Queue appointmentConfirmedQueue,
                                               DirectExchange agendamentoExchange) {
        return BindingBuilder.bind(appointmentConfirmedQueue)
                .to(agendamentoExchange)
                .with(RK_APPOINTMENT_CONFIRMED);
    }

    @Bean
    public Binding bindingAppointmentCancelled(Queue appointmentCancelledQueue,
                                               DirectExchange agendamentoExchange) {
        return BindingBuilder.bind(appointmentCancelledQueue)
                .to(agendamentoExchange)
                .with(RK_APPOINTMENT_CANCELLED);
    }

    @Bean
    public Binding bindingAppointmentRescheduled(Queue appointmentRescheduledQueue,
                                                 DirectExchange agendamentoExchange) {
        return BindingBuilder.bind(appointmentRescheduledQueue)
                .to(agendamentoExchange)
                .with(RK_APPOINTMENT_RESCHEDULED);
    }

    @Bean
    public Binding bindingAppointmentAttended(Queue appointmentAttendedQueue,
                                              DirectExchange agendamentoExchange) {
        return BindingBuilder.bind(appointmentAttendedQueue)
                .to(agendamentoExchange)
                .with(RK_APPOINTMENT_ATTENDED);
    }

    @Bean
    public Binding bindingAppointmentNoSlot(Queue appointmentNoSlotQueue,
                                            DirectExchange agendamentoExchange) {
        return BindingBuilder.bind(appointmentNoSlotQueue)
                .to(agendamentoExchange)
                .with(RK_APPOINTMENT_NO_SLOT);
    }

    @Bean
    public Binding bindingAppointmentExpired(Queue appointmentExpiredQueue,
                                             DirectExchange agendamentoExchange) {
        return BindingBuilder.bind(appointmentExpiredQueue)
                .to(agendamentoExchange)
                .with(RK_APPOINTMENT_EXPIRED);
    }

    @Bean
    public Binding bindingPatientNoShow(Queue patientNoShowQueue,
                                        DirectExchange agendamentoExchange) {
        return BindingBuilder.bind(patientNoShowQueue)
                .to(agendamentoExchange)
                .with(RK_PATIENT_NO_SHOW);
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
