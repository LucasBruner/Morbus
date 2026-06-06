package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.domain.exception.QueueEmptyException;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CallNextPatient")
class CallNextPatientTest {

    @Mock
    private IQueueEntryRepository repository;

    @Mock
    private IQueueEventPublisher publisher;

    private CallNextPatient useCase;

    @BeforeEach
    void setUp() {
        useCase = CallNextPatient.create(repository, publisher);
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Patient buildPatient() {
        return Patient.builder()
                .id(UUID.randomUUID())
                .nome("Maria Oliveira")
                .cpf("987.654.321-00")
                .dataNascimento(LocalDate.of(1965, 3, 10))
                .grupoLegal(EPriorityGroup.IDOSO)
                .contato("maria@email.com")
                .build();
    }

    private QueueEntry buildEntry(EQueueStatus status, ERiskColor color) {
        return QueueEntry.builder()
                .id(UUID.randomUUID())
                .patient(buildPatient())
                .riskColor(color)
                .queueStatus(status)
                .registeredAt(LocalDateTime.now().minusHours(2))
                .build();
    }

    // ── Fluxo feliz ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando há paciente aguardando")
    class QuandoHaPaciente {

        @Test
        @DisplayName("deve retornar a entrada chamada")
        void deveRetornarEntradaChamada() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO, ERiskColor.VERMELHO);
            when(repository.findNextByPriority()).thenReturn(Optional.of(entry));

            QueueEntry result = useCase.run();

            assertThat(result).isEqualTo(entry);
        }

        @Test
        @DisplayName("deve alterar o status para AGENDADO")
        void deveAlterarStatusParaAgendado() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO, ERiskColor.AZUL);
            when(repository.findNextByPriority()).thenReturn(Optional.of(entry));

            useCase.run();

            assertThat(entry.getQueueStatus()).isEqualTo(EQueueStatus.AGENDADO);
        }

        @Test
        @DisplayName("deve preencher updatedAt")
        void devePreencherUpdatedAt() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO, ERiskColor.AMARELO);
            when(repository.findNextByPriority()).thenReturn(Optional.of(entry));

            LocalDateTime antes = LocalDateTime.now();
            useCase.run();

            assertThat(entry.getUpdatedAt()).isNotNull();
            assertThat(entry.getUpdatedAt()).isAfterOrEqualTo(antes);
        }

        @Test
        @DisplayName("deve salvar a entrada atualizada no repositório")
        void deveSalvarEntradaAtualizada() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO, ERiskColor.VERDE);
            when(repository.findNextByPriority()).thenReturn(Optional.of(entry));

            useCase.run();

            verify(repository).save(entry);
        }

        @Test
        @DisplayName("deve publicar o evento no broker")
        void devePublicarEvento() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO, ERiskColor.VERMELHO);
            when(repository.findNextByPriority()).thenReturn(Optional.of(entry));

            useCase.run();

            verify(publisher).publish(entry);
        }

        @Test
        @DisplayName("deve salvar antes de publicar o evento")
        void deveSalvarAntesDePublicar() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO, ERiskColor.AZUL);
            when(repository.findNextByPriority()).thenReturn(Optional.of(entry));

            useCase.run();

            InOrder order = inOrder(repository, publisher);
            order.verify(repository).save(entry);
            order.verify(publisher).publish(entry);
        }
    }

    // ── Fila vazia ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando a fila está vazia")
    class QuandoFilaVazia {

        @Test
        @DisplayName("deve lançar QueueEmptyException")
        void deveLancarQueueEmptyException() {
            when(repository.findNextByPriority()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.run())
                    .isInstanceOf(QueueEmptyException.class)
                    .hasMessageContaining("pacientes");
        }

        @Test
        @DisplayName("não deve chamar save quando a fila está vazia")
        void naoDeveChamarSave() {
            when(repository.findNextByPriority()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.run())
                    .isInstanceOf(QueueEmptyException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("não deve publicar evento quando a fila está vazia")
        void naoDevePublicarEvento() {
            when(repository.findNextByPriority()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.run())
                    .isInstanceOf(QueueEmptyException.class);

            verify(publisher, never()).publish(any());
        }
    }
}
