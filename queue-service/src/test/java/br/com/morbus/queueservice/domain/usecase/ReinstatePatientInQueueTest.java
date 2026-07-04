package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.domain.exception.QueueNotAllowedException;
import br.com.morbus.queueservice.domain.exception.QueueNotExistException;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReinstatePatientInQueue")
class ReinstatePatientInQueueTest {

    @Mock
    private IQueueEntryRepository queueEntryRepository;

    @Mock
    private IQueueEventPublisher eventPublisher;

    private ReinstatePatientInQueue useCase;

    @BeforeEach
    void setUp() {
        useCase = ReinstatePatientInQueue.create(queueEntryRepository, eventPublisher);
    }

    private QueueEntry buildEntry(EQueueStatus status) {
        return QueueEntry.builder()
                .id(UUID.randomUUID())
                .patient(Patient.builder()
                        .id(UUID.randomUUID())
                        .nome("João")
                        .sobrenome("Silva")
                        .dataNascimento(LocalDate.of(1990, 5, 20))
                        .grupoLegal(EPriorityGroup.GERAL)
                        .build())
                .procedure(Procedure.builder().id(UUID.randomUUID()).noProcedimento("Consulta").build())
                .riskColor(ERiskColor.AMARELO)
                .queueStatus(status)
                .registeredAt(LocalDateTime.now().minusDays(2))
                .build();
    }

    @Nested
    @DisplayName("quando status é AGENDADO")
    class QuandoAgendado {

        @Test
        @DisplayName("deve retornar a entrada com status AGUARDANDO")
        void deveReinserirNaFila() {
            QueueEntry entry = buildEntry(EQueueStatus.AGENDADO);
            when(queueEntryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

            QueueEntry result = useCase.execute(entry.getId());

            assertThat(result.getQueueStatus()).isEqualTo(EQueueStatus.AGUARDANDO);
        }

        @Test
        @DisplayName("deve mover registeredAt para o fim da fila (agora)")
        void deveAtualizarRegisteredAt() {
            QueueEntry entry = buildEntry(EQueueStatus.AGENDADO);
            LocalDateTime registeredAtOriginal = entry.getRegisteredAt();
            when(queueEntryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

            LocalDateTime antes = LocalDateTime.now();
            useCase.execute(entry.getId());

            assertThat(entry.getRegisteredAt()).isAfter(registeredAtOriginal);
            assertThat(entry.getRegisteredAt()).isAfterOrEqualTo(antes);
        }

        @Test
        @DisplayName("deve salvar antes de publicar PATIENT_REINSTATED")
        void deveSalvarAntesDePublicar() {
            QueueEntry entry = buildEntry(EQueueStatus.AGENDADO);
            when(queueEntryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

            useCase.execute(entry.getId());

            InOrder order = inOrder(queueEntryRepository, eventPublisher);
            order.verify(queueEntryRepository).save(entry);
            order.verify(eventPublisher).publishPatientReinstated(entry);
        }
    }

    @Nested
    @DisplayName("quando a entrada não existe")
    class QuandoNaoExiste {

        @Test
        @DisplayName("deve lançar QueueNotExistException")
        void deveLancarQueueNotExistException() {
            UUID id = UUID.randomUUID();
            when(queueEntryRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(id))
                    .isInstanceOf(QueueNotExistException.class);

            verify(queueEntryRepository, never()).save(any());
            verify(eventPublisher, never()).publishPatientReinstated(any());
        }
    }

    @Nested
    @DisplayName("quando status não permite reinserção")
    class QuandoStatusNaoPermitido {

        @ParameterizedTest(name = "status {0} deve lançar QueueNotAllowedException")
        @EnumSource(value = EQueueStatus.class, names = {"AGUARDANDO", "ATENDIDO", "CANCELADO", "FALTOU", "DEVOLVIDO"})
        @DisplayName("deve lançar QueueNotAllowedException para status inválidos")
        void deveLancarQueueNotAllowedException(EQueueStatus status) {
            QueueEntry entry = buildEntry(status);
            when(queueEntryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

            assertThatThrownBy(() -> useCase.execute(entry.getId()))
                    .isInstanceOf(QueueNotAllowedException.class);

            verify(queueEntryRepository, never()).save(any());
            verify(eventPublisher, never()).publishPatientReinstated(any());
        }
    }
}
