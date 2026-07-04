package br.com.sus.notificationservice.consumer;

import br.com.sus.notificationservice.model.dto.AppointmentConfirmedEventDTO;
import br.com.sus.notificationservice.model.dto.AppointmentNoSlotEventDTO;
import br.com.sus.notificationservice.model.dto.QueueEventDTO;
import br.com.sus.notificationservice.model.dto.SolicitacaoDevolvidaEventDTO;
import br.com.sus.notificationservice.model.dto.SolicitacaoNegadaEventDTO;
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
@DisplayName("NotificationEventConverter")
class NotificationEventConverterTest {

    private NotificationEventConverter converter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        converter = new NotificationEventConverter();
        try {
            var field = NotificationEventConverter.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(converter, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("canConvert()")
    class CanConvert {

        @Test
        @DisplayName("deve retornar true para todos os DTOs de evento suportados")
        void deveRetornarTrueParaTiposSuportados() {
            @SuppressWarnings("unchecked")
            Message<?> message = mock(Message.class);

            assertThat(converter.canConvert(message, QueueEventDTO.class)).isTrue();
            assertThat(converter.canConvert(message, SolicitacaoNegadaEventDTO.class)).isTrue();
            assertThat(converter.canConvert(message, SolicitacaoDevolvidaEventDTO.class)).isTrue();
            assertThat(converter.canConvert(message, AppointmentConfirmedEventDTO.class)).isTrue();
            assertThat(converter.canConvert(message, AppointmentNoSlotEventDTO.class)).isTrue();
        }

        @Test
        @DisplayName("deve retornar false quando o tipo alvo nao e suportado")
        void deveRetornarFalseParaOutroTipo() {
            @SuppressWarnings("unchecked")
            Message<?> message = mock(Message.class);

            boolean result = converter.canConvert(message, String.class);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("convert()")
    class Convert {

        @Test
        @DisplayName("deve desserializar payload bytes em QueueEventDTO valido")
        @SuppressWarnings("unchecked")
        void deveDesserializarQueueEventDTO() throws Exception {
            QueueEventDTO original = new QueueEventDTO(
                    "PATIENT_CALLED",
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    "Ana Costa",
                    "ana@email.com",
                    "Consulta Neurológica",
                    "AMARELO",
                    LocalDateTime.of(2025, 6, 1, 8, 0)
            );
            Message<?> result = convertRoundTrip(original, QueueEventDTO.class);

            QueueEventDTO dto = (QueueEventDTO) result.getPayload();
            assertThat(dto.eventType()).isEqualTo("PATIENT_CALLED");
            assertThat(dto.patientName()).isEqualTo("Ana Costa");
        }

        @Test
        @DisplayName("deve desserializar payload bytes em SolicitacaoNegadaEventDTO valido")
        @SuppressWarnings("unchecked")
        void deveDesserializarSolicitacaoNegada() throws Exception {
            SolicitacaoNegadaEventDTO original = new SolicitacaoNegadaEventDTO(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    "Sem indicação clínica", LocalDateTime.of(2026, 6, 12, 11, 30));

            Message<?> result = convertRoundTrip(original, SolicitacaoNegadaEventDTO.class);

            SolicitacaoNegadaEventDTO dto = (SolicitacaoNegadaEventDTO) result.getPayload();
            assertThat(dto.justificativa()).isEqualTo("Sem indicação clínica");
        }

        @Test
        @DisplayName("deve desserializar payload bytes em AppointmentConfirmedEventDTO valido")
        @SuppressWarnings("unchecked")
        void deveDesserializarAppointmentConfirmed() throws Exception {
            AppointmentConfirmedEventDTO original = new AppointmentConfirmedEventDTO(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    LocalDateTime.of(2026, 7, 10, 8, 30));

            Message<?> result = convertRoundTrip(original, AppointmentConfirmedEventDTO.class);

            AppointmentConfirmedEventDTO dto = (AppointmentConfirmedEventDTO) result.getPayload();
            assertThat(dto.agendadoEm()).isEqualTo(LocalDateTime.of(2026, 7, 10, 8, 30));
        }

        @Test
        @DisplayName("deve lançar RuntimeException quando o payload nao e JSON valido")
        @SuppressWarnings("unchecked")
        void deveLancarExcecaoParaPayloadInvalido() {
            byte[] payloadInvalido = "nao-eh-json".getBytes();

            Message<byte[]> message = mock(Message.class);
            when(message.getPayload()).thenReturn(payloadInvalido);

            assertThatThrownBy(() -> converter.convert(message, QueueEventDTO.class))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Erro ao desserializar evento de notificacao");
        }

        @SuppressWarnings("unchecked")
        private <T> Message<?> convertRoundTrip(T original, Class<T> type) throws Exception {
            byte[] payload = objectMapper.writeValueAsBytes(original);

            Message<byte[]> message = mock(Message.class);
            when(message.getPayload()).thenReturn(payload);
            when(message.withPayload(org.mockito.ArgumentMatchers.any()))
                    .thenAnswer(inv -> {
                        Message<Object> converted = mock(Message.class);
                        when(converted.getPayload()).thenReturn(inv.getArgument(0));
                        return converted;
                    });

            return converter.convert(message, type);
        }
    }
}
