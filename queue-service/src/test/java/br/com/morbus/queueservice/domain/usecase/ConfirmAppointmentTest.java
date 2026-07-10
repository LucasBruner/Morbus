package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfirmAppointment")
class ConfirmAppointmentTest {

    @Mock
    private IQueueEntryRepository queueEntryRepository;

    private ConfirmAppointment useCase;

    @BeforeEach
    void setUp() {
        useCase = ConfirmAppointment.create(queueEntryRepository);
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
                .registeredAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Nested
    @DisplayName("quando status é CHAMADO")
    class QuandoChamado {

        @Test
        @DisplayName("deve retornar a entrada com status AGENDADO")
        void deveConfirmarAgendamento() {
            QueueEntry entry = buildEntry(EQueueStatus.CHAMADO);
            when(queueEntryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

            QueueEntry result = useCase.execute(entry.getId());

            assertThat(result.getQueueStatus()).isEqualTo(EQueueStatus.AGENDADO);
        }

        @Test
        @DisplayName("deve salvar a entrada atualizada no repositório")
        void deveSalvarEntradaAtualizada() {
            QueueEntry entry = buildEntry(EQueueStatus.CHAMADO);
            when(queueEntryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

            useCase.execute(entry.getId());

            verify(queueEntryRepository).save(entry);
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
        }
    }

    @Nested
    @DisplayName("quando status não permite confirmação")
    class QuandoStatusNaoPermitido {

        @ParameterizedTest(name = "status {0} deve lançar QueueNotAllowedException")
        @EnumSource(value = EQueueStatus.class,
                names = {"AGUARDANDO", "AGENDADO", "ATENDIDO", "CANCELADO", "FALTOU", "DEVOLVIDO"})
        @DisplayName("deve lançar QueueNotAllowedException para status inválidos")
        void deveLancarQueueNotAllowedException(EQueueStatus status) {
            QueueEntry entry = buildEntry(status);
            when(queueEntryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

            assertThatThrownBy(() -> useCase.execute(entry.getId()))
                    .isInstanceOf(QueueNotAllowedException.class);

            verify(queueEntryRepository, never()).save(any());
        }
    }
}
