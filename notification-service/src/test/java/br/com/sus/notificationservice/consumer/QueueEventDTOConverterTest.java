package br.com.sus.notificationservice.consumer;

import br.com.sus.notificationservice.model.dto.QueueEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueEventDTOConverter")
class QueueEventDTOConverterTest {

    private QueueEventDTOConverter converter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        converter = new QueueEventDTOConverter();
        // injeta o ObjectMapper manualmente (sem CDI no teste unitário)
        try {
            var field = QueueEventDTOConverter.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(converter, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── canConvert() ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("canConvert()")
    class CanConvert {

        @Test
        @DisplayName("deve retornar true quando o tipo alvo é QueueEventDTO")
        void deveRetornarTrueParaQueueEventDTO() {
            @SuppressWarnings("unchecked")
            Message<?> message = mock(Message.class);

            boolean result = converter.canConvert(message, QueueEventDTO.class);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("deve retornar false quando o tipo alvo não é QueueEventDTO")
        void deveRetornarFalseParaOutroTipo() {
            @SuppressWarnings("unchecked")
            Message<?> message = mock(Message.class);

            boolean result = converter.canConvert(message, String.class);

            assertThat(result).isFalse();
        }
    }

    // ── convert() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("convert()")
    class Convert {

        @Test
        @DisplayName("deve desserializar payload bytes em QueueEventDTO válido")
        @SuppressWarnings("unchecked")
        void deveDesserializarPayloadValido() throws Exception {
            QueueEventDTO original = new QueueEventDTO(
                    "PATIENT_CALLED",
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    "Ana Costa",
                    "ana@email.com",
                    "Consulta Neurológica",
                    "AMARELO",
                    LocalDateTime.of(2025, 6, 1, 8, 0)
            );
            byte[] payload = objectMapper.writeValueAsBytes(original);

            Message<byte[]> message = mock(Message.class);
            when(message.getPayload()).thenReturn(payload);
            when(message.withPayload(org.mockito.ArgumentMatchers.any()))
                    .thenAnswer(inv -> {
                        Message<QueueEventDTO> converted = mock(Message.class);
                        when(converted.getPayload()).thenReturn(inv.getArgument(0));
                        return converted;
                    });

            Message<?> result = converter.convert(message, QueueEventDTO.class);
            QueueEventDTO dto = (QueueEventDTO) result.getPayload();

            assertThat(dto.eventType()).isEqualTo("PATIENT_CALLED");
            assertThat(dto.patientName()).isEqualTo("Ana Costa");
            assertThat(dto.patientContact()).isEqualTo("ana@email.com");
            assertThat(dto.procedureName()).isEqualTo("Consulta Neurológica");
            assertThat(dto.riskColor()).isEqualTo("AMARELO");
            assertThat(dto.queueEntryId())
                    .isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        }

        @Test
        @DisplayName("deve lançar RuntimeException quando o payload não é JSON válido")
        @SuppressWarnings("unchecked")
        void deveLancarExcecaoParaPayloadInvalido() {
            byte[] payloadInvalido = "nao-eh-json".getBytes();

            Message<byte[]> message = mock(Message.class);
            when(message.getPayload()).thenReturn(payloadInvalido);

            assertThatThrownBy(() -> converter.convert(message, QueueEventDTO.class))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Erro ao desserializar QueueEventDTO");
        }

        @Test
        @DisplayName("deve lançar RuntimeException quando o payload está vazio")
        @SuppressWarnings("unchecked")
        void deveLancarExcecaoParaPayloadVazio() {
            byte[] payloadVazio = new byte[0];

            Message<byte[]> message = mock(Message.class);
            when(message.getPayload()).thenReturn(payloadVazio);

            assertThatThrownBy(() -> converter.convert(message, QueueEventDTO.class))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Erro ao desserializar QueueEventDTO");
        }
    }
}
