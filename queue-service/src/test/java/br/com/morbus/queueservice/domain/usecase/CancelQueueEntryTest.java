package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.domain.exception.QueueNotAllowedException;
import br.com.morbus.queueservice.domain.exception.QueueNotExistException;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.usecase.dto.QueueCancelDTO;
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
@DisplayName("CancelQueueEntry")
class CancelQueueEntryTest {

    @Mock
    private IQueueEntryRepository repository;

    @Mock
    private IQueueEventPublisher publisher;

    private CancelQueueEntry useCase;

    @BeforeEach
    void setUp() {
        useCase = CancelQueueEntry.create(repository, publisher);
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private Patient buildPatient() {
        return Patient.builder()
                .id(UUID.randomUUID())
                .nome("João")
                .sobrenome("Silva")
                .cpf("123.456.789-00")
                .dataNascimento(LocalDate.of(1990, 5, 20))
                .grupoLegal(EPriorityGroup.GERAL)
                .contato("joao@email.com")
                .build();
    }

    private QueueEntry buildEntry(EQueueStatus status) {
        return QueueEntry.builder()
                .id(UUID.randomUUID())
                .patient(buildPatient())
                .procedure(Procedure.builder()
                        .id(UUID.randomUUID())
                        .noProcedimento("Consulta de Cardiologia")
                        .coProcedimento("0301010072")
                        .build())
                .riskColor(ERiskColor.AZUL)
                .queueStatus(status)
                .registeredAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    private QueueCancelDTO buildDTO(UUID id) {
        return new QueueCancelDTO(id, "Paciente desistiu");
    }

    private QueueCancelDTO buildDTOSemMotivo(UUID id) {
        return new QueueCancelDTO(id, null);
    }

    // ── Fluxo feliz — AGUARDANDO ──────────────────────────────────────────────

    @Nested
    @DisplayName("quando status é AGUARDANDO")
    class QuandoAguardando {

        @Test
        @DisplayName("deve retornar a entrada com status CANCELADO")
        void deveRetornarEntradaCancelada() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO);
            QueueCancelDTO dto = buildDTO(entry.getId());
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));

            QueueEntry result = useCase.run(dto);

            assertThat(result.getQueueStatus()).isEqualTo(EQueueStatus.CANCELADO);
        }

        @Test
        @DisplayName("deve preencher updatedAt")
        void devePreencherUpdatedAt() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO);
            QueueCancelDTO dto = buildDTO(entry.getId());
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));

            LocalDateTime antes = LocalDateTime.now();
            useCase.run(dto);

            assertThat(entry.getUpdatedAt()).isNotNull();
            assertThat(entry.getUpdatedAt()).isAfterOrEqualTo(antes);
        }

        @Test
        @DisplayName("deve salvar a entrada no repositório")
        void deveSalvarEntrada() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO);
            QueueCancelDTO dto = buildDTO(entry.getId());
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));

            useCase.run(dto);

            verify(repository).save(entry);
        }

        @Test
        @DisplayName("deve publicar evento com o motivo")
        void devePublicarEventoComMotivo() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO);
            QueueCancelDTO dto = buildDTO(entry.getId());
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));

            useCase.run(dto);

            verify(publisher).publishPatientCancelled(entry, dto.reason());
        }

        @Test
        @DisplayName("deve salvar antes de publicar o evento")
        void deveSalvarAntesDePublicar() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO);
            QueueCancelDTO dto = buildDTO(entry.getId());
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));

            useCase.run(dto);

            InOrder order = inOrder(repository, publisher);
            order.verify(repository).save(entry);
            order.verify(publisher).publishPatientCancelled(entry, dto.reason());
        }

        @Test
        @DisplayName("deve publicar evento mesmo com motivo nulo")
        void devePublicarComMotivoNulo() {
            QueueEntry entry = buildEntry(EQueueStatus.AGUARDANDO);
            QueueCancelDTO dto = buildDTOSemMotivo(entry.getId());
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));

            useCase.run(dto);

            verify(publisher).publishPatientCancelled(entry, null);
        }
    }

    // ── Fluxo feliz — AGENDADO ────────────────────────────────────────────────

    @Nested
    @DisplayName("quando status é AGENDADO")
    class QuandoAgendado {

        @Test
        @DisplayName("deve cancelar entrada com status AGENDADO")
        void deveCancelarEntradaAgendada() {
            QueueEntry entry = buildEntry(EQueueStatus.AGENDADO);
            QueueCancelDTO dto = buildDTO(entry.getId());
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));

            QueueEntry result = useCase.run(dto);

            assertThat(result.getQueueStatus()).isEqualTo(EQueueStatus.CANCELADO);
            verify(repository).save(entry);
            verify(publisher).publishPatientCancelled(entry, dto.reason());
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

            assertThatThrownBy(() -> useCase.run(new QueueCancelDTO(id, "motivo")))
                    .isInstanceOf(QueueNotExistException.class);
        }

        @Test
        @DisplayName("não deve salvar quando entrada não existe")
        void naoDeveSalvar() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.run(new QueueCancelDTO(id, "motivo")))
                    .isInstanceOf(QueueNotExistException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("não deve publicar quando entrada não existe")
        void naoDevePublicar() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.run(new QueueCancelDTO(id, "motivo")))
                    .isInstanceOf(QueueNotExistException.class);

            verify(publisher, never()).publishPatientCancelled(any(), any());
        }
    }

    // ── Status não permitido ──────────────────────────────────────────────────

    @Nested
    @DisplayName("quando status não permite cancelamento")
    class QuandoStatusNaoPermitido {

        @ParameterizedTest(name = "status {0} deve lançar QueueNotAllowedException")
        @EnumSource(value = EQueueStatus.class, names = {"ATENDIDO", "CANCELADO", "FALTOU", "DEVOLVIDO"})
        @DisplayName("deve lançar QueueNotAllowedException para status inválidos")
        void deveLancarQueueNotAllowedException(EQueueStatus status) {
            QueueEntry entry = buildEntry(status);
            QueueCancelDTO dto = buildDTO(entry.getId());
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));

            assertThatThrownBy(() -> useCase.run(dto))
                    .isInstanceOf(QueueNotAllowedException.class);
        }

        @Test
        @DisplayName("não deve salvar quando status não permite cancelamento")
        void naoDeveSalvar() {
            QueueEntry entry = buildEntry(EQueueStatus.ATENDIDO);
            QueueCancelDTO dto = buildDTO(entry.getId());
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));

            assertThatThrownBy(() -> useCase.run(dto))
                    .isInstanceOf(QueueNotAllowedException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("não deve publicar quando status não permite cancelamento")
        void naoDevePublicar() {
            QueueEntry entry = buildEntry(EQueueStatus.CANCELADO);
            QueueCancelDTO dto = buildDTO(entry.getId());
            when(repository.findById(entry.getId())).thenReturn(Optional.of(entry));

            assertThatThrownBy(() -> useCase.run(dto))
                    .isInstanceOf(QueueNotAllowedException.class);

            verify(publisher, never()).publishPatientCancelled(any(), any());
        }
    }
}
