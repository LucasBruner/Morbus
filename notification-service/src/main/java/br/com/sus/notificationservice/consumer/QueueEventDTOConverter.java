package br.com.sus.notificationservice.consumer;

import br.com.sus.notificationservice.model.dto.QueueEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.MessageConverter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.lang.reflect.Type;

@ApplicationScoped
public class QueueEventDTOConverter implements MessageConverter {

    @Inject
    ObjectMapper objectMapper;

    @Override
    public boolean canConvert(Message<?> message, Type target) {
        return target == QueueEventDTO.class;
    }

    @Override
    public Message<?> convert(Message<?> message, Type type) {
        try {
            byte[] payload = (byte[]) message.getPayload();
            QueueEventDTO dto = objectMapper.readValue(payload, QueueEventDTO.class);
            return message.withPayload(dto);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao desserializar QueueEventDTO", e);
        }
    }
}
