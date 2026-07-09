package br.com.sus.notificationservice.integration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * Gerenciador de ciclo de vida dos containers Testcontainers para os testes de integração
 * do notification-service.
 *
 * <p>Inicia um PostgreSQL real e um RabbitMQ real para que os testes exercitem a stack
 * completa sem H2 e sem broker simulado.</p>
 */
public class NotificationContainerResource implements QuarkusTestResourceLifecycleManager {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sus_notification_db")
                    .withUsername("sus_user")
                    .withPassword("sus_pass");

    private static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:3-management-alpine")
                    .withUser("admin", "admin");

    @Override
    public Map<String, String> start() {
        POSTGRES.start();
        RABBIT.start();

        Map<String, String> props = new HashMap<>();

        // Datasource — PostgreSQL real
        props.put("quarkus.datasource.db-kind", "postgresql");
        props.put("quarkus.datasource.jdbc.url", POSTGRES.getJdbcUrl());
        props.put("quarkus.datasource.username", POSTGRES.getUsername());
        props.put("quarkus.datasource.password", POSTGRES.getPassword());
        props.put("quarkus.hibernate-orm.database.generation", "drop-and-create");

        // RabbitMQ — canal queue-events
        String host = RABBIT.getHost();
        String port = String.valueOf(RABBIT.getAmqpPort());
        String user = "admin";
        String pass = "admin";

        for (String channel : new String[]{"queue-events",
                "solicitation-denied-events",
                "solicitation-devolved-events",
                "appointment-confirmed-events",
                "appointment-no-slot-events"}) {
            props.put("mp.messaging.incoming." + channel + ".host", host);
            props.put("mp.messaging.incoming." + channel + ".port", port);
            props.put("mp.messaging.incoming." + channel + ".username", user);
            props.put("mp.messaging.incoming." + channel + ".password", pass);
        }

        // Expõe host e porta para que os testes possam publicar mensagens AMQP diretamente
        props.put("it.rabbitmq.host", host);
        props.put("it.rabbitmq.port", port);
        props.put("it.rabbitmq.username", user);
        props.put("it.rabbitmq.password", pass);

        return props;
    }

    @Override
    public void stop() {
        if (POSTGRES.isRunning()) POSTGRES.stop();
        if (RABBIT.isRunning()) RABBIT.stop();
    }

    /** Expõe o container RabbitMQ para injeção direta nos testes via {@code inject()}. */
    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(RABBIT,
                new TestInjector.AnnotatedAndMatchesType(InjectRabbit.class, RabbitMQContainer.class));
    }
}
