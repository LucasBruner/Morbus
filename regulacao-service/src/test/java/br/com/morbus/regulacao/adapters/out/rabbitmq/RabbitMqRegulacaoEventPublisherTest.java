package br.com.morbus.regulacao.adapters.out.rabbitmq;

import br.com.morbus.regulacao.adapters.out.rabbitmq.dto.SolicitacaoAprovadaPayload;
import br.com.morbus.regulacao.adapters.out.rabbitmq.dto.SolicitacaoDevolvidaPayload;
import br.com.morbus.regulacao.adapters.out.rabbitmq.dto.SolicitacaoNegadaPayload;
import br.com.morbus.regulacao.adapters.out.rabbitmq.dto.SolicitacaoReclassificadaPayload;
import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMqRegulacaoEventPublisher")
class RabbitMqRegulacaoEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitMqRegulacaoEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RabbitMqRegulacaoEventPublisher(rabbitTemplate);
    }

    private Solicitacao buildSolicitacao() {
        return new Solicitacao(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                EStatusSolicitacao.APROVADA, ERiscoSolicitado.AMARELO, "I10", "justificativa", "Dr. Silva", null,
                EDestino.FILA_REGULADA, "motivo", UUID.randomUUID(),
                LocalDateTime.now().minusHours(1), LocalDateTime.now().minusHours(1), null
        );
    }

    @Nested
    @DisplayName("publishSolicitacaoAprovada")
    class PublishSolicitacaoAprovada {

        @Test
        @DisplayName("deve publicar no exchange e routing key corretos com o payload esperado")
        void devePublicar() {
            Solicitacao s = buildSolicitacao();
            publisher.publishSolicitacaoAprovada(s);

            ArgumentCaptor<SolicitacaoAprovadaPayload> captor = ArgumentCaptor.forClass(SolicitacaoAprovadaPayload.class);
            verify(rabbitTemplate).convertAndSend(eq("sus.regulacao.exchange"), eq("solicitation.approved"), captor.capture());

            SolicitacaoAprovadaPayload payload = captor.getValue();
            assertThat(payload.solicitacaoId()).isEqualTo(s.getId());
            assertThat(payload.patientId()).isEqualTo(s.getPatientId());
            assertThat(payload.procedureId()).isEqualTo(s.getProcedureId());
            assertThat(payload.unitExecutanteId()).isEqualTo(s.getUnidadeExecutanteId());
            assertThat(payload.tipoFila()).isEqualTo(EDestino.FILA_REGULADA);
            assertThat(payload.riskColor()).isEqualTo(ERiscoSolicitado.AMARELO);
        }
    }

    @Nested
    @DisplayName("publishSolicitacaoNegada")
    class PublishSolicitacaoNegada {

        @Test
        @DisplayName("deve publicar no exchange e routing key corretos com o payload esperado")
        void devePublicar() {
            Solicitacao s = buildSolicitacao();
            LocalDateTime antes = LocalDateTime.now();
            publisher.publishSolicitacaoNegada(s);
            LocalDateTime depois = LocalDateTime.now();

            ArgumentCaptor<SolicitacaoNegadaPayload> captor = ArgumentCaptor.forClass(SolicitacaoNegadaPayload.class);
            verify(rabbitTemplate).convertAndSend(eq("sus.regulacao.exchange"), eq("solicitation.denied"), captor.capture());

            SolicitacaoNegadaPayload payload = captor.getValue();
            assertThat(payload.solicitacaoId()).isEqualTo(s.getId());
            assertThat(payload.justificativa()).isEqualTo(s.getJustificativaNegacao());
            assertThat(payload.negadoEm()).isAfterOrEqualTo(antes).isBeforeOrEqualTo(depois);
        }
    }

    @Nested
    @DisplayName("publishSolicitacaoDevolvida")
    class PublishSolicitacaoDevolvida {

        @Test
        @DisplayName("deve publicar no exchange e routing key corretos com o payload esperado")
        void devePublicar() {
            Solicitacao s = buildSolicitacao();
            publisher.publishSolicitacaoDevolvida(s);

            ArgumentCaptor<SolicitacaoDevolvidaPayload> captor = ArgumentCaptor.forClass(SolicitacaoDevolvidaPayload.class);
            verify(rabbitTemplate).convertAndSend(eq("sus.regulacao.exchange"), eq("solicitation.devolved"), captor.capture());

            SolicitacaoDevolvidaPayload payload = captor.getValue();
            assertThat(payload.solicitacaoId()).isEqualTo(s.getId());
            assertThat(payload.justificativa()).isEqualTo(s.getJustificativaNegacao());
        }
    }

    @Nested
    @DisplayName("publishSolicitacaoReclassificada")
    class PublishSolicitacaoReclassificada {

        @Test
        @DisplayName("deve publicar no exchange e routing key corretos com o payload esperado")
        void devePublicar() {
            Solicitacao s = buildSolicitacao();
            LocalDateTime antes = LocalDateTime.now();
            publisher.publishSolicitacaoReclassificada(s);
            LocalDateTime depois = LocalDateTime.now();

            ArgumentCaptor<SolicitacaoReclassificadaPayload> captor = ArgumentCaptor.forClass(SolicitacaoReclassificadaPayload.class);
            verify(rabbitTemplate).convertAndSend(eq("sus.regulacao.exchange"), eq("solicitation.reclassified"), captor.capture());

            SolicitacaoReclassificadaPayload payload = captor.getValue();
            assertThat(payload.solicitacaoId()).isEqualTo(s.getId());
            assertThat(payload.novoRiskColor()).isEqualTo(s.getRiskColor());
            assertThat(payload.reclassificadoEm()).isAfterOrEqualTo(antes).isBeforeOrEqualTo(depois);
        }
    }
}
