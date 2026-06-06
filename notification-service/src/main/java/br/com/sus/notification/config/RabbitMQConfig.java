package br.com.sus.notification.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

@ApplicationScoped
public class RabbitMQConfig {

    @ConfigProperty(name = "rabbitmq.host", defaultValue = "localhost")
    String host;

    @ConfigProperty(name = "rabbitmq.port", defaultValue = "5672")
    int port;

    @ConfigProperty(name = "rabbitmq.username", defaultValue = "admin")
    String username;

    @ConfigProperty(name = "rabbitmq.password", defaultValue = "admin")
    String password;

    @Produces
    @ApplicationScoped
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        return factory;
    }
}
