package br.com.morbus.queueservice;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class QueueServiceApplicationTests {

    @MockitoBean
    RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
    }

}
