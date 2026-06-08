package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.domain.exception.QueueNotAllowedException;
import br.com.morbus.queueservice.domain.exception.QueueNotExistException;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.usecase.DTO.QueueEntryRiskQueuePosition;
import br.com.morbus.queueservice.domain.usecase.DTO.QueueUpdateRiskColorDTO;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReclassifyPriority")
class ReclassifyPriorityTest {

    @Mock
    private IQueueEntryRepository repository;

    @Mock
    private IQueueEventPublisher publisher;

    private ReclassifyPriority useCase;

    @BeforeEach
    void setUp() {
        useCase = ReclassifyPriority.create(publisher, repository);
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private QueueEntry buildEntry(EQueueStatus status, ERiskColor color) {
        Patient patient = Patient.builder()
                .id(UUID.randomUUID())
                .nome("Carlos")
                .sobrenome("Ferreira")
                .cpf("999.888.777-66")
                .dataNascimento(LocalDate.of(1970, 1, 1))
                .grupoLegal(EPriorityGroup.GERAL)
                .contato("carlos@email.com")
                .build();

        return QueueEntry.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .riskColor(color)
                .queueStatus(status)
                .registeredAt(LocalDateTime.now().minusHours(2))
                .build();
    }

    private QueueUpdateRiskColorDTO buildDTO(UUID id, ERiskColor novaColor) {
        return new QueueUpdateRiskColorDTO(id, novaColor);
    }

    // ── Fluxo feliz — AGUARDANDO ──────────────────────────────────────────────

    @Nested
    @DisplayName("quando status é AGUARDANDO")
    class QuandoAguardando {

        @Test
        @DisplayName("deve atualizar a riskColor")
        void deveAtualizarRiskColor() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO, ERiskColor.AZUL);
            QueueUpdateRiskColorDTO dto = buildDTO(entry.getId(), ERiskColor.VERMELHO);
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));
            when(repository.countEntriesWithHigherPriority(entry)).thenReturn(2);

            useCase.run(dto);

            assertThat(entry.getRiskColor()).isEqualTo(ERiskColor.VERMELHO);
        }

        @Test
        @DisplayName("deve preencher updatedAt")
        void devePreencherUpdatedAt() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO, ERiskColor.VERDE);
            QueueUpdateRiskColorDTO dto = buildDTO(entry.getId(), ERiskColor.AMARELO);
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));
            when(repository.countEntriesWithHigherPriority(entry)).thenReturn(0);

            LocalDateTime antes = LocalDateTime.now();
            useCase.run(dto);

            assertThat(entry.getUpdatedAt()).isNotNull();
            assertThat(entry.getUpdatedAt()).isAfterOrEqualTo(antes);
        }

        @Test
        @DisplayName("deve salvar a entrada no repositório")
        void deveSalvarEntrada() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO, ERiskColor.AZUL);
            QueueUpdateRiskColorDTO dto = buildDTO(entry.getId(), ERiskColor.VERMELHO);
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));
            when(repository.countEntriesWithHigherPriority(entry)).thenReturn(1);

            useCase.run(dto);

            verify(repository).save(entry);
        }

        @Test
        @DisplayName("deve publicar evento de prioridade atualizada")
        void devePublicarEvento() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO, ERiskColor.AZUL);
            QueueUpdateRiskColorDTO dto = buildDTO(entry.getId(), ERiskColor.AMARELO);
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));
            when(repository.countEntriesWithHigherPriority(entry)).thenReturn(0);

            useCase.run(dto);

            verify(publisher).publishPriorityUpdated(entry);
        }

        @Test
        @DisplayName("deve salvar antes de publicar o evento")
        void deveSalvarAntesDePublicar() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO, ERiskColor.AZUL);
            QueueUpdateRiskColorDTO dto = buildDTO(entry.getId(), ERiskColor.VERMELHO);
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));
            when(repository.countEntriesWithHigherPriority(entry)).thenReturn(0);

            useCase.run(dto);

            InOrder order = inOrder(repository, publisher);
            order.verify(repository).save(entry);
            order.verify(publisher).publishPriorityUpdated(entry);
        }

        @Test
        @DisplayName("deve retornar a posição calculada pelo repositório")
        void deveRetornarPosicaoCalculada() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO, ERiskColor.AZUL);
            QueueUpdateRiskColorDTO dto = buildDTO(entry.getId(), ERiskColor.VERMELHO);
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));
            when(repository.countEntriesWithHigherPriority(entry)).thenReturn(4);

            QueueEntryRiskQueuePosition result = useCase.run(dto);

            assertThat(result.posicaoCalculada()).isEqualTo(4);
            assertThat(result.queueEntry()).isEqualTo(entry);
        }
    }

    // ── Fluxo feliz — DEVOLVIDO ───────────────────────────────────────────────

    @Nested
    @DisplayName("quando status é DEVOLVIDO")
    class QuandoDevolvido {

        @Test
        @DisplayName("deve reclassificar entrada com status DEVOLVIDO")
        void deveReclassificarDevolvido() {
            QueueEntry entry = buildEntry(EQueueStatus.DEVOLVIDO, ERiskColor.VERDE);
            QueueUpdateRiskColorDTO dto = buildDTO(entry.getId(), ERiskColor.VERMELHO);
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));
            when(repository.countEntriesWithHigherPriority(entry)).thenReturn(0);

            useCase.run(dto);

            assertThat(entry.getRiskColor()).isEqualTo(ERiskColor.VERMELHO);
            verify(repository).save(entry);
            verify(publisher).publishPriorityUpdated(entry);
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

            assertThatThrownBy(() -> useCase.run(buildDTO(id, ERiskColor.VERMELHO)))
                    .isInstanceOf(QueueNotExistException.class);
        }

        @Test
        @DisplayName("não deve salvar quando entrada não existe")
        void naoDeveSalvar() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.run(buildDTO(id, ERiskColor.VERMELHO)))
                    .isInstanceOf(QueueNotExistException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("não deve publicar quando entrada não existe")
        void naoDevePublicar() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.run(buildDTO(id, ERiskColor.VERMELHO)))
                    .isInstanceOf(QueueNotExistException.class);

            verify(publisher, never()).publishPriorityUpdated(any());
        }
    }

    // ── Status não permitido ──────────────────────────────────────────────────

    @Nested
    @DisplayName("quando status não permite reclassificação")
    class QuandoStatusNaoPermitido {

        @ParameterizedTest(name = "status {0} deve lançar QueueNotAllowedException")
        @EnumSource(value = EQueueStatus.class, names = {"AGENDADO", "ATENDIDO", "FALTOU", "CANCELADO"})
        @DisplayName("deve lançar QueueNotAllowedException para status inválidos")
        void deveLancarQueueNotAllowedException(EQueueStatus status) {
            QueueEntry entry = buildEntry(status, ERiskColor.AZUL);
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));

            assertThatThrownBy(() -> useCase.run(buildDTO(entry.getId(), ERiskColor.VERMELHO)))
                    .isInstanceOf(QueueNotAllowedException.class);
        }

        @Test
        @DisplayName("não deve salvar quando status não permite reclassificação")
        void naoDeveSalvar() {
            QueueEntry entry = buildEntry(EQueueStatus.ATENDIDO, ERiskColor.AZUL);
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));

            assertThatThrownBy(() -> useCase.run(buildDTO(entry.getId(), ERiskColor.VERMELHO)))
                    .isInstanceOf(QueueNotAllowedException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("não deve publicar quando status não permite reclassificação")
        void naoDevePublicar() {
            QueueEntry entry = buildEntry(EQueueStatus.CANCELADO, ERiskColor.AZUL);
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));

            assertThatThrownBy(() -> useCase.run(buildDTO(entry.getId(), ERiskColor.VERMELHO)))
                    .isInstanceOf(QueueNotAllowedException.class);

            verify(publisher, never()).publishPriorityUpdated(any());
        }
    }
}
