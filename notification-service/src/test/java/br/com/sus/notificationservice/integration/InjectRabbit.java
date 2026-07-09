package br.com.sus.notificationservice.integration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marcador para injeção do {@link org.testcontainers.containers.RabbitMQContainer}
 * no contexto de testes via {@link NotificationContainerResource}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectRabbit {
}
