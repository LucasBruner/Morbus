package br.com.morbus.queueservice.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchanges publicados pelo queue-service
    public static final String QUEUE_EXCHANGE = "sus.queue.exchange";
    public static final String QUEUE_DLX = "sus.queue.dlx";

    // Exchanges de origem (declarados localmente para permitir o binding neste contexto Spring)
    public static final String REGULACAO_EXCHANGE = "sus.regulacao.exchange";
    public static final String AGENDAMENTO_EXCHANGE = "sus.agendamento.exchange";

    // Routing keys consumidas
    public static final String RK_SOLICITATION_APPROVED = "solicitation.approved";
    public static final String RK_APPOINTMENT_EXPIRED = "appointment.expired";
    public static final String RK_PATIENT_NO_SHOW = "patient.no_show";
    public static final String RK_APPOINTMENT_CONFIRMED = "appointment.confirmed";
    public static final String RK_APPOINTMENT_CANCELLED = "appointment.cancelled";
    public static final String RK_APPOINTMENT_NO_SLOT = "appointment.no_slot";

    // Filas consumidas
    public static final String QUEUE_SOLICITATION_APPROVED = "queue.solicitation.approved";
    public static final String QUEUE_APPOINTMENT_EXPIRED = "queue.appointment.expired";
    public static final String QUEUE_PATIENT_NO_SHOW = "queue.patient.no_show";
    public static final String QUEUE_APPOINTMENT_CONFIRMED = "queue.appointment.confirmed";
    public static final String QUEUE_APPOINTMENT_CANCELLED = "queue.appointment.cancelled";
    public static final String QUEUE_APPOINTMENT_NO_SLOT = "queue.appointment.no_slot";

    public static final String DLQ_SOLICITATION_APPROVED = "queue.solicitation.approved.dlq";
    public static final String DLQ_APPOINTMENT_EXPIRED = "queue.appointment.expired.dlq";
    public static final String DLQ_PATIENT_NO_SHOW = "queue.patient.no_show.dlq";
    public static final String DLQ_APPOINTMENT_CONFIRMED = "queue.appointment.confirmed.dlq";
    public static final String DLQ_APPOINTMENT_CANCELLED = "queue.appointment.cancelled.dlq";
    public static final String DLQ_APPOINTMENT_NO_SLOT = "queue.appointment.no_slot.dlq";

    @Bean
    public DirectExchange queueExchange() {
        return new DirectExchange(QUEUE_EXCHANGE);
    }

    @Bean
    public DirectExchange priorityExchange() {
        return new DirectExchange("sus.queue.priority.exchange");
    }

    @Bean
    public DirectExchange queueDlx() {
        return new DirectExchange(QUEUE_DLX, true, false);
    }

    @Bean
    public DirectExchange regulacaoExchange() {
        return new DirectExchange(REGULACAO_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange agendamentoExchange() {
        return new DirectExchange(AGENDAMENTO_EXCHANGE, true, false);
    }

    @Bean
    public Queue patientRegisteredQueue() {
        return new Queue("queue.patient.registered", true);
    }

    @Bean
    public Binding patientRegisteredBinding(Queue patientRegisteredQueue,
                                            DirectExchange queueExchange) {
        return BindingBuilder
                .bind(patientRegisteredQueue)
                .to(queueExchange)
                .with("patient.registered");
    }

    // ─── Filas consumidas ──────────────────────────────────────────────────────

    @Bean
    public Queue solicitationApprovedQueue() {
        return QueueBuilder.durable(QUEUE_SOLICITATION_APPROVED)
                .withArgument("x-dead-letter-exchange", QUEUE_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_SOLICITATION_APPROVED)
                .build();
    }

    @Bean
    public Queue appointmentExpiredQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_EXPIRED)
                .withArgument("x-dead-letter-exchange", QUEUE_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_APPOINTMENT_EXPIRED)
                .build();
    }

    @Bean
    public Queue patientNoShowQueue() {
        return QueueBuilder.durable(QUEUE_PATIENT_NO_SHOW)
                .withArgument("x-dead-letter-exchange", QUEUE_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_PATIENT_NO_SHOW)
                .build();
    }

    @Bean
    public Queue appointmentConfirmedQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_CONFIRMED)
                .withArgument("x-dead-letter-exchange", QUEUE_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_APPOINTMENT_CONFIRMED)
                .build();
    }

    @Bean
    public Queue appointmentCancelledQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_CANCELLED)
                .withArgument("x-dead-letter-exchange", QUEUE_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_APPOINTMENT_CANCELLED)
                .build();
    }

    @Bean
    public Queue appointmentNoSlotQueue() {
        return QueueBuilder.durable(QUEUE_APPOINTMENT_NO_SLOT)
                .withArgument("x-dead-letter-exchange", QUEUE_DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_APPOINTMENT_NO_SLOT)
                .build();
    }

    @Bean
    public Queue solicitationApprovedDlq() {
        return QueueBuilder.durable(DLQ_SOLICITATION_APPROVED).build();
    }

    @Bean
    public Queue appointmentExpiredDlq() {
        return QueueBuilder.durable(DLQ_APPOINTMENT_EXPIRED).build();
    }

    @Bean
    public Queue patientNoShowDlq() {
        return QueueBuilder.durable(DLQ_PATIENT_NO_SHOW).build();
    }

    @Bean
    public Queue appointmentConfirmedDlq() {
        return QueueBuilder.durable(DLQ_APPOINTMENT_CONFIRMED).build();
    }

    @Bean
    public Queue appointmentCancelledDlq() {
        return QueueBuilder.durable(DLQ_APPOINTMENT_CANCELLED).build();
    }

    @Bean
    public Queue appointmentNoSlotDlq() {
        return QueueBuilder.durable(DLQ_APPOINTMENT_NO_SLOT).build();
    }

    @Bean
    public Binding bindingSolicitationApproved(Queue solicitationApprovedQueue,
                                               DirectExchange regulacaoExchange) {
        return BindingBuilder.bind(solicitationApprovedQueue)
                .to(regulacaoExchange)
                .with(RK_SOLICITATION_APPROVED);
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
    public Binding bindingAppointmentNoSlot(Queue appointmentNoSlotQueue,
                                            DirectExchange agendamentoExchange) {
        return BindingBuilder.bind(appointmentNoSlotQueue)
                .to(agendamentoExchange)
                .with(RK_APPOINTMENT_NO_SLOT);
    }

    @Bean
    public Binding bindingSolicitationApprovedDlq(Queue solicitationApprovedDlq,
                                                  DirectExchange queueDlx) {
        return BindingBuilder.bind(solicitationApprovedDlq)
                .to(queueDlx)
                .with(DLQ_SOLICITATION_APPROVED);
    }

    @Bean
    public Binding bindingAppointmentExpiredDlq(Queue appointmentExpiredDlq,
                                                DirectExchange queueDlx) {
        return BindingBuilder.bind(appointmentExpiredDlq)
                .to(queueDlx)
                .with(DLQ_APPOINTMENT_EXPIRED);
    }

    @Bean
    public Binding bindingPatientNoShowDlq(Queue patientNoShowDlq,
                                           DirectExchange queueDlx) {
        return BindingBuilder.bind(patientNoShowDlq)
                .to(queueDlx)
                .with(DLQ_PATIENT_NO_SHOW);
    }

    @Bean
    public Binding bindingAppointmentConfirmedDlq(Queue appointmentConfirmedDlq,
                                                  DirectExchange queueDlx) {
        return BindingBuilder.bind(appointmentConfirmedDlq)
                .to(queueDlx)
                .with(DLQ_APPOINTMENT_CONFIRMED);
    }

    @Bean
    public Binding bindingAppointmentCancelledDlq(Queue appointmentCancelledDlq,
                                                  DirectExchange queueDlx) {
        return BindingBuilder.bind(appointmentCancelledDlq)
                .to(queueDlx)
                .with(DLQ_APPOINTMENT_CANCELLED);
    }

    @Bean
    public Binding bindingAppointmentNoSlotDlq(Queue appointmentNoSlotDlq,
                                               DirectExchange queueDlx) {
        return BindingBuilder.bind(appointmentNoSlotDlq)
                .to(queueDlx)
                .with(DLQ_APPOINTMENT_NO_SLOT);
    }
}
