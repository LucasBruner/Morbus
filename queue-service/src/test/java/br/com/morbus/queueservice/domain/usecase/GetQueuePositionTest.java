package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.exception.QueueNotExistException;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.usecase.DTO.QueueEntryRiskQueuePosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
@DisplayName("GetQueuePosition")
class GetQueuePositionTest {

    @Mock
    private IQueueEntryRepository repository;

    private GetQueuePosition useCase;

    @BeforeEach
    void setUp() {
        useCase = GetQueuePosition.create(repository);
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private QueueEntry buildEntry() {
        Patient patient = Patient.builder()
                .id(UUID.randomUUID())
                .nome("Ana")
                .sobrenome("Costa")
                .cpf("111.222.333-44")
                .dataNascimento(LocalDate.of(1985, 8, 15))
                .grupoLegal(EPriorityGroup.GERAL)
                .contato("ana@email.com")
                .build();

        return QueueEntry.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .riskColor(ERiskColor.VERDE)
                .queueStatus(EQueueStatus.AGUARDANDO)
                .registeredAt(LocalDateTime.now().minusMinutes(30))
                .build();
    }

    // ── Fluxo feliz ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando a entrada existe")
    class QuandoEntradaExiste {

        @Test
        @DisplayName("deve retornar QueueEntryRiskQueuePosition com a entrada correta")
        void deveRetornarResultadoComEntrada() {
            QueueEntry entry = buildEntry();
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));
            when(repository.countEntriesWithHigherPriority(entry)).thenReturn(3);

            QueueEntryRiskQueuePosition result = useCase.run(entry.getId());

            assertThat(result.queueEntry()).isEqualTo(entry);
        }

        @Test
        @DisplayName("deve retornar posição calculada pelo repositório")
        void deveRetornarPosicaoCalculada() {
            QueueEntry entry = buildEntry();
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));
            when(repository.countEntriesWithHigherPriority(entry)).thenReturn(5);

            QueueEntryRiskQueuePosition result = useCase.run(entry.getId());

            assertThat(result.posicaoCalculada()).isEqualTo(5);
        }

        @Test
        @DisplayName("deve chamar countEntriesWithHigherPriority com a entrada encontrada")
        void deveChamarCountComEntradaCorreta() {
            QueueEntry entry = buildEntry();
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));
            when(repository.countEntriesWithHigherPriority(entry)).thenReturn(0);

            useCase.run(entry.getId());

            verify(repository).countEntriesWithHigherPriority(entry);
        }

        @Test
        @DisplayName("deve retornar posição 0 quando nenhum paciente tem prioridade maior")
        void deveRetornarPosicaoZeroQuandoPrimeiro() {
            QueueEntry entry = buildEntry();
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));
            when(repository.countEntriesWithHigherPriority(entry)).thenReturn(0);

            QueueEntryRiskQueuePosition result = useCase.run(entry.getId());

            assertThat(result.posicaoCalculada()).isZero();
        }
    }

    // ── Entrada não existe ────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando a entrada não existe")
    class QuandoNaoExiste {

        @Test
        @DisplayName("deve lançar QueueNotExistException")
        void deveLancarQueueNotExistException() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.run(id))
                    .isInstanceOf(QueueNotExistException.class);
        }

        @Test
        @DisplayName("não deve chamar countEntriesWithHigherPriority quando entrada não existe")
        void naoDeveChamarCount() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.run(id))
                    .isInstanceOf(QueueNotExistException.class);

            verify(repository, never()).countEntriesWithHigherPriority(any());
        }
    }
}
