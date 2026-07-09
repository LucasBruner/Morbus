package br.com.morbus.queueservice.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base para todos os testes de integração com Testcontainers.
 *
 * <p>Inicia um PostgreSQL e um RabbitMQ reais (via Docker) compartilhados por toda
 * a suite de testes de integração. Os containers são criados uma única vez e
 * reutilizados entre as subclasses para reduzir o tempo de startup.</p>
 *
 * <p>Perfil {@code integration} habilita Flyway real e desabilita as sobreposições
 * de H2 definidas em {@code application.properties} de teste.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Testcontainers
public abstract class AbstractContainerIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sus_queue_db")
                    .withUsername("sus_user")
                    .withPassword("sus_pass");

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:3-management-alpine")
                    .withUser("admin", "admin");

    @LocalServerPort
    protected int port;
}
