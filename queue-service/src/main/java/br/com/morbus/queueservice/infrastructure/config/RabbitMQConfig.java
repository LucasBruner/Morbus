package br.com.morbus.queueservice.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange queueExchange() {
        return new DirectExchange("sus.queue.exchange");
    }

    @Bean
    public DirectExchange priorityExchange() {
        return new DirectExchange("sus.queue.priority.exchange");
    }

    @Bean
    public Queue patientRegisteredQueue() {
        return new Queue("queue.patient.registered", true);
    }

    @Bean
    public Queue patientCalledQueue() {
        return new Queue("queue.patient.called", true);
    }

    @Bean
    public Binding patientRegisteredBinding(Queue patientRegisteredQueue,
                                            DirectExchange queueExchange) {
        return BindingBuilder
                .bind(patientRegisteredQueue)
                .to(queueExchange)
                .with("patient.registered");
    }
}
