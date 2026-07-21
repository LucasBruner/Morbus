package br.com.sus.notificationservice.consumer;

import br.com.sus.notificationservice.model.dto.AppointmentConfirmedEventDTO;
import br.com.sus.notificationservice.model.dto.AppointmentNoSlotEventDTO;
import br.com.sus.notificationservice.model.dto.QueueEventDTO;
import br.com.sus.notificationservice.model.dto.SolicitacaoDevolvidaEventDTO;
import br.com.sus.notificationservice.model.dto.SolicitacaoNegadaEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.MessageConverter;
import io.vertx.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.lang.reflect.Type;
import java.util.Set;

@ApplicationScoped
public class NotificationEventConverter implements MessageConverter {

    private static final Set<Type> SUPPORTED_TYPES = Set.of(
            QueueEventDTO.class,
            SolicitacaoNegadaEventDTO.class,
            SolicitacaoDevolvidaEventDTO.class,
            AppointmentConfirmedEventDTO.class,
            AppointmentNoSlotEventDTO.class
    );

    @Inject
    ObjectMapper objectMapper;

    @Override
    public boolean canConvert(Message<?> message, Type target) {
        return SUPPORTED_TYPES.contains(target);
    }

    @Override
    public Message<?> convert(Message<?> message, Type type) {
        try {
            Object rawPayload = message.getPayload();
            byte[] payload = rawPayload instanceof Buffer buffer ? buffer.getBytes() : (byte[]) rawPayload;
            Object dto = objectMapper.readValue(payload, (Class<?>) type);
            return message.withPayload(dto);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao desserializar evento de notificacao", e);
        }
    }
}
