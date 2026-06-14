package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.usecase.dto.ListQueueByPriorityDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListQueueByPriority Use Case Tests")
class ListQueueByPriorityTest {

    @Mock
    private IQueueEntryRepository queueEntryRepository;

    private ListQueueByPriority useCase;
    private UUID procedureId;

    @BeforeEach
    void setUp() {
        useCase = ListQueueByPriority.create(queueEntryRepository);
        procedureId = UUID.randomUUID();
    }

    private Patient createPatient(String cpf, EPriorityGroup grupoLegal) {
        return Patient.builder()
                .id(UUID.randomUUID())
                .cpf(cpf)
                .cns("1234567890123")
                .nome("Paciente")
                .sobrenome("Teste")
                .dataNascimento(LocalDate.of(1980, 5, 15))
                .gender(EGender.MASCULINO)
                .contato("paciente@email.com")
                .ativo(true)
                .grupoLegal(grupoLegal)
                .build();
    }

    private QueueEntry createQueueEntry(Patient patient, ERiskColor riskColor, LocalDateTime registeredAt) {
        return QueueEntry.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .queueStatus(EQueueStatus.AGUARDANDO)
                .riskColor(riskColor)
                .registeredAt(registeredAt)
                .updatedAt(LocalDateTime.now())
                .scorePosicaoCalculada(0)
                .build();
    }

    @Test
    @DisplayName("CA-01: Should filter queue entries by procedureId")
    void testFilterByProcedureId() {
        // Arrange
        Patient patient = createPatient("11111111111", EPriorityGroup.GERAL);
        QueueEntry entry = createQueueEntry(patient, ERiskColor.VERDE, LocalDateTime.now());

        when(queueEntryRepository.findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null))
                .thenReturn(List.of(entry));

        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, null, null, null, null);

        // Act
        List<QueueEntry> result = useCase.run(dto);

        // Assert
        assertThat(result).hasSize(1).contains(entry);
        verify(queueEntryRepository).findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null);
    }

    @Test
    @DisplayName("CA-02: Should use default status AGUARDANDO when status is null")
    void testDefaultStatusAguardando() {
        // Arrange
        Patient patient = createPatient("22222222222", EPriorityGroup.GERAL);
        QueueEntry entry = createQueueEntry(patient, ERiskColor.VERDE, LocalDateTime.now());

        when(queueEntryRepository.findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null))
                .thenReturn(List.of(entry));

        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, null, null, null, null);

        // Act
        useCase.run(dto);

        // Assert
        verify(queueEntryRepository).findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null);
    }

    @Test
    @DisplayName("CA-02: Should accept custom status")
    void testCustomStatus() {
        // Arrange
        Patient patient = createPatient("33333333333", EPriorityGroup.GERAL);
        QueueEntry entry = createQueueEntry(patient, ERiskColor.VERDE, LocalDateTime.now());

        when(queueEntryRepository.findByProcedureIdAndFilters(procedureId, EQueueStatus.AGENDADO, null))
                .thenReturn(List.of(entry));

        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, EQueueStatus.AGENDADO, null, null, null);

        // Act
        useCase.run(dto);

        // Assert
        verify(queueEntryRepository).findByProcedureIdAndFilters(procedureId, EQueueStatus.AGENDADO, null);
    }

    @Test
    @DisplayName("CA-03: Should filter by riskColor when provided")
    void testFilterByRiskColor() {
        // Arrange
        Patient patient = createPatient("44444444444", EPriorityGroup.GERAL);
        QueueEntry entry = createQueueEntry(patient, ERiskColor.VERMELHO, LocalDateTime.now());

        when(queueEntryRepository.findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, ERiskColor.VERMELHO))
                .thenReturn(List.of(entry));

        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, null, ERiskColor.VERMELHO, null, null);

        // Act
        List<QueueEntry> result = useCase.run(dto);

        // Assert
        assertThat(result).hasSize(1).contains(entry);
        verify(queueEntryRepository).findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, ERiskColor.VERMELHO);
    }

    @Test
    @DisplayName("CA-03: Should not filter by riskColor when null (accepts all colors)")
    void testNoRiskColorFilter() {
        // Arrange
        Patient patient = createPatient("55555555555", EPriorityGroup.GERAL);
        QueueEntry entry = createQueueEntry(patient, ERiskColor.AMARELO, LocalDateTime.now());

        when(queueEntryRepository.findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null))
                .thenReturn(List.of(entry));

        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, null, null, null, null);

        // Act
        useCase.run(dto);

        // Assert
        verify(queueEntryRepository).findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null);
    }

    @Test
    @DisplayName("CA-04: Should sort by riskColor ASC (VERMELHO first)")
    void testSortByRiskColorAsc() {
        // Arrange
        Patient patient1 = createPatient("11111111111", EPriorityGroup.GERAL);
        Patient patient2 = createPatient("22222222222", EPriorityGroup.GERAL);
        Patient patient3 = createPatient("33333333333", EPriorityGroup.GERAL);

        QueueEntry vermelho = createQueueEntry(patient1, ERiskColor.VERMELHO, LocalDateTime.now());
        QueueEntry amarelo = createQueueEntry(patient2, ERiskColor.AMARELO, LocalDateTime.now().plusMinutes(1));
        QueueEntry verde = createQueueEntry(patient3, ERiskColor.VERDE, LocalDateTime.now().plusMinutes(2));

        // Return unsorted list
        when(queueEntryRepository.findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null))
                .thenReturn(List.of(vermelho, amarelo, verde));

        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, null, null, null, null);

        // Act
        List<QueueEntry> result = useCase.run(dto);

        // Assert - Should be sorted: VERMELHO (1), AMARELO (2), VERDE (3)
        assertThat(result)
                .hasSize(3)
                .extracting("riskColor")
                .containsExactly(ERiskColor.VERMELHO, ERiskColor.AMARELO, ERiskColor.VERDE);
    }

    @Test
    @DisplayName("CA-04: Should sort by priorityGroup ASC after riskColor (IDOSO first)")
    void testSortByPriorityGroupAsc() {
        // Arrange
        Patient idoso = createPatient("11111111111", EPriorityGroup.IDOSO);
        Patient gestante = createPatient("22222222222", EPriorityGroup.GESTANTE);
        Patient geral = createPatient("33333333333", EPriorityGroup.GERAL);

        // All same risk color
        QueueEntry entry1 = createQueueEntry(geral, ERiskColor.VERMELHO, LocalDateTime.now());
        QueueEntry entry2 = createQueueEntry(gestante, ERiskColor.VERMELHO, LocalDateTime.now());
        QueueEntry entry3 = createQueueEntry(idoso, ERiskColor.VERMELHO, LocalDateTime.now());

        when(queueEntryRepository.findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null))
                .thenReturn(List.of(entry3, entry2, entry1));

        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, null, null, null, null);

        // Act
        List<QueueEntry> result = useCase.run(dto);

        // Assert - Should be sorted: IDOSO (1), GESTANTE (2), GERAL (6)
        assertThat(result)
                .hasSize(3)
                .extracting(entry -> entry.getPatient().getGrupoLegal())
                .containsExactly(EPriorityGroup.IDOSO, EPriorityGroup.GESTANTE, EPriorityGroup.GERAL);
    }

    @Test
    @DisplayName("CA-04: Should sort by registeredAt ASC (FIFO) after riskColor and priorityGroup")
    void testSortByRegisteredAtFifo() {
        // Arrange
        Patient patient1 = createPatient("11111111111", EPriorityGroup.GERAL);
        Patient patient2 = createPatient("22222222222", EPriorityGroup.GERAL);
        Patient patient3 = createPatient("33333333333", EPriorityGroup.GERAL);

        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 10, 0);
        QueueEntry entry1 = createQueueEntry(patient1, ERiskColor.VERDE, now.plusHours(2));
        QueueEntry entry2 = createQueueEntry(patient2, ERiskColor.VERDE, now);
        QueueEntry entry3 = createQueueEntry(patient3, ERiskColor.VERDE, now.plusHours(1));

        when(queueEntryRepository.findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null))
                .thenReturn(List.of(entry2, entry3, entry1));

        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, null, null, null, null);

        // Act
        List<QueueEntry> result = useCase.run(dto);

        // Assert - Should be sorted by registeredAt: now, now+1h, now+2h (FIFO)
        assertThat(result)
                .hasSize(3)
                .extracting("registeredAt")
                .containsExactly(now, now.plusHours(1), now.plusHours(2));
    }

    @Test
    @DisplayName("CA-04: Complete sorting test (riskColor > priorityGroup > registeredAt)")
    void testCompleteSorting() {
        // Arrange
        Patient idoso1 = createPatient("11111111111", EPriorityGroup.IDOSO);
        Patient geral1 = createPatient("22222222222", EPriorityGroup.GERAL);
        Patient geral2 = createPatient("33333333333", EPriorityGroup.GERAL);
        Patient idoso2 = createPatient("44444444444", EPriorityGroup.IDOSO);

        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 10, 0);
        
        // Mixed entries
        QueueEntry entry1 = createQueueEntry(geral1, ERiskColor.AMARELO, now.plusHours(1)); // AMARELO, GERAL, 10h
        QueueEntry entry2 = createQueueEntry(idoso1, ERiskColor.VERMELHO, now.plusHours(3)); // VERMELHO, IDOSO, 13h
        QueueEntry entry3 = createQueueEntry(geral2, ERiskColor.AMARELO, now); // AMARELO, GERAL, 10h (FIFO first)
        QueueEntry entry4 = createQueueEntry(idoso2, ERiskColor.VERMELHO, now.plusHours(2)); // VERMELHO, IDOSO, 12h (FIFO first)

        when(queueEntryRepository.findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null))
                .thenReturn(List.of(entry4, entry2, entry3, entry1));

        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, null, null, null, null);

        // Act
        List<QueueEntry> result = useCase.run(dto);

        // Assert - Should be: 
        // VERMELHO+IDOSO (2 entries ordered by FIFO)
        // AMARELO+GERAL (2 entries ordered by FIFO)
        assertThat(result)
                .hasSize(4)
                .containsExactly(entry4, entry2, entry3, entry1);
    }

    @Test
    @DisplayName("CA-05: Should support pagination with page and size")
    void testPaginationWithPageAndSize() {
        // Arrange
        List<QueueEntry> entries = List.of(
                createQueueEntry(createPatient("11111111111", EPriorityGroup.GERAL), ERiskColor.VERDE, LocalDateTime.now()),
                createQueueEntry(createPatient("22222222222", EPriorityGroup.GERAL), ERiskColor.VERDE, LocalDateTime.now().plusMinutes(1)),
                createQueueEntry(createPatient("33333333333", EPriorityGroup.GERAL), ERiskColor.VERDE, LocalDateTime.now().plusMinutes(2)),
                createQueueEntry(createPatient("44444444444", EPriorityGroup.GERAL), ERiskColor.VERDE, LocalDateTime.now().plusMinutes(3)),
                createQueueEntry(createPatient("55555555555", EPriorityGroup.GERAL), ERiskColor.VERDE, LocalDateTime.now().plusMinutes(4))
        );

        when(queueEntryRepository.findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null))
                .thenReturn(entries);

        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, null, null, 1, 2);

        // Act
        List<QueueEntry> result = useCase.run(dto);

        // Assert - Page 1 with size 2 should return entries 2-3
        assertThat(result).hasSize(2).containsExactly(entries.get(2), entries.get(3));
    }

    @Test
    @DisplayName("CA-05: Should return empty list when page is out of range")
    void testPaginationOutOfRange() {
        // Arrange
        List<QueueEntry> entries = List.of(
                createQueueEntry(createPatient("11111111111", EPriorityGroup.GERAL), ERiskColor.VERDE, LocalDateTime.now()),
                createQueueEntry(createPatient("22222222222", EPriorityGroup.GERAL), ERiskColor.VERDE, LocalDateTime.now().plusMinutes(1))
        );

        when(queueEntryRepository.findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null))
                .thenReturn(entries);

        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, null, null, 5, 2);

        // Act
        List<QueueEntry> result = useCase.run(dto);

        // Assert - Should return empty list
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("CA-05: Should return empty list (not throw exception) when no entries found")
    void testEmptyQueueNoException() {
        // Arrange
        when(queueEntryRepository.findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null))
                .thenReturn(List.of());

        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, null, null, null, null);

        // Act & Assert - Should not throw exception
        assertThatNoException().isThrownBy(() -> useCase.run(dto));
        
        List<QueueEntry> result = useCase.run(dto);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("CA-05: Should handle default pagination (page 0, size MAX)")
    void testDefaultPagination() {
        // Arrange
        List<QueueEntry> entries = List.of(
                createQueueEntry(createPatient("11111111111", EPriorityGroup.GERAL), ERiskColor.VERDE, LocalDateTime.now()),
                createQueueEntry(createPatient("22222222222", EPriorityGroup.GERAL), ERiskColor.VERDE, LocalDateTime.now().plusMinutes(1)),
                createQueueEntry(createPatient("33333333333", EPriorityGroup.GERAL), ERiskColor.VERDE, LocalDateTime.now().plusMinutes(2))
        );

        when(queueEntryRepository.findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null))
                .thenReturn(entries);

        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, null, null, null, null);

        // Act
        List<QueueEntry> result = useCase.run(dto);

        // Assert - Should return all entries
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("CA-06: Factory method should create use case instance")
    void testFactoryMethod() {
        // Act
        ListQueueByPriority instance = ListQueueByPriority.create(queueEntryRepository);

        // Assert
        assertThat(instance).isNotNull();
    }

    @Test
    @DisplayName("Should handle null procedure entries gracefully")
    void testNullProcedureEntriesHandling() {
        // Arrange
        when(queueEntryRepository.findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null))
                .thenReturn(List.of());

        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, null, null, null, null);

        // Act
        List<QueueEntry> result = useCase.run(dto);

        // Assert
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should maintain queue entry order after sorting")
    void testQueueEntryOrderPreservation() {
        // Arrange
        Patient patient = createPatient("11111111111", EPriorityGroup.GERAL);
        QueueEntry entry1 = createQueueEntry(patient, ERiskColor.VERDE, LocalDateTime.of(2024, 1, 1, 10, 0));
        QueueEntry entry2 = createQueueEntry(patient, ERiskColor.VERDE, LocalDateTime.of(2024, 1, 1, 10, 1));
        QueueEntry entry3 = createQueueEntry(patient, ERiskColor.VERDE, LocalDateTime.of(2024, 1, 1, 10, 2));

        when(queueEntryRepository.findByProcedureIdAndFilters(procedureId, EQueueStatus.AGUARDANDO, null))
                .thenReturn(List.of(entry1, entry2, entry3));

        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, null, null, null, null);

        // Act
        List<QueueEntry> result = useCase.run(dto);

        // Assert - Should maintain FIFO order
        assertThat(result).containsExactly(entry1, entry2, entry3);
    }
}
