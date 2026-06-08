package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.exception.PatientAlreadyInactiveException;
import br.com.morbus.queueservice.domain.exception.PatientNotFoundException;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InactivatePatient Use Case Tests")
class InactivatePatientTest {

    @Mock
    private IPatientRepository patientRepository;

    @Mock
    private IQueueEntryRepository queueEntryRepository;

    private InactivatePatient useCase;
    private UUID patientId;
    private Patient activePatient;

    @BeforeEach
    void setUp() {
        useCase = InactivatePatient.create(patientRepository, queueEntryRepository);
        patientId = UUID.randomUUID();

        activePatient = Patient.builder()
                .id(patientId)
                .cpf("12345678900")
                .cns("1234567890123")
                .nome("João")
                .sobrenome("Silva")
                .dataNascimento(LocalDate.of(1980, 5, 15))
                .gender(EGender.MASCULINO)
                .contato("joao@email.com")
                .ativo(true)
                .grupoLegal(EPriorityGroup.GERAL)
                .build();
    }

    @Test
    @DisplayName("Lança PatientNotFoundException quando paciente não é encontrado")
    void testPatientNotFound() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> useCase.run(patientId))
                .isInstanceOf(PatientNotFoundException.class)
                .hasMessage("Paciente não cadastrado");

        verify(patientRepository, times(1)).findById(patientId);
        verify(patientRepository, never()).save(any());
        verify(queueEntryRepository, never()).findByPatientAndStatusIn(any(), any());
    }

    @Test
    @DisplayName("Lança PatientAlreadyInactiveException quando paciente já está inativo")
    void testPatientAlreadyInactive() {
        Patient inactivePatient = Patient.builder()
                .id(patientId)
                .cpf("12345678900")
                .cns("1234567890123")
                .nome("João")
                .sobrenome("Silva")
                .dataNascimento(LocalDate.of(1980, 5, 15))
                .gender(EGender.MASCULINO)
                .contato("joao@email.com")
                .ativo(false)
                .grupoLegal(EPriorityGroup.GERAL)
                .build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(inactivePatient));

        assertThatThrownBy(() -> useCase.run(patientId))
                .isInstanceOf(PatientAlreadyInactiveException.class)
                .hasMessage("Paciente já está inativado");

        verify(patientRepository, times(1)).findById(patientId);
        verify(patientRepository, never()).save(any());
        verify(queueEntryRepository, never()).findByPatientAndStatusIn(any(), any());
    }

    @Test
    @DisplayName("Deve inativar paciente com sucesso")
    void testInactivateActivePatientSuccessfully() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(activePatient));
        when(queueEntryRepository.findByPatientAndStatusIn(activePatient, List.of(EQueueStatus.AGUARDANDO, EQueueStatus.AGENDADO)))
                .thenReturn(List.of());

        Patient result = useCase.run(patientId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(patientId);
        assertThat(result.isAtivo()).isFalse();

        verify(patientRepository, times(1)).findById(patientId);
        verify(queueEntryRepository, times(1)).findByPatientAndStatusIn(activePatient, List.of(EQueueStatus.AGUARDANDO, EQueueStatus.AGENDADO));
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    @DisplayName("Deve cancelar todas as filas do paciente com status AGUARDANDO")
    void testCancelAllAguardandoQueueEntries() {
        QueueEntry entry1 = QueueEntry.builder()
                .id(UUID.randomUUID())
                .patient(activePatient)
                .queueStatus(EQueueStatus.AGUARDANDO)
                .riskColor(ERiskColor.VERMELHO)
                .registeredAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        QueueEntry entry2 = QueueEntry.builder()
                .id(UUID.randomUUID())
                .patient(activePatient)
                .queueStatus(EQueueStatus.AGUARDANDO)
                .riskColor(ERiskColor.AMARELO)
                .registeredAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<QueueEntry> pendingEntries = List.of(entry1, entry2);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(activePatient));
        when(queueEntryRepository.findByPatientAndStatusIn(activePatient, List.of(EQueueStatus.AGUARDANDO, EQueueStatus.AGENDADO)))
                .thenReturn(pendingEntries);

        useCase.run(patientId);

        verify(patientRepository, times(1)).findById(patientId);
        verify(queueEntryRepository, times(1)).findByPatientAndStatusIn(activePatient, List.of(EQueueStatus.AGUARDANDO, EQueueStatus.AGENDADO));
        verify(queueEntryRepository, times(2)).save(any(QueueEntry.class));
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    @DisplayName("Deve cancelar todas as filas do paciente com status AGENDADO")
    void testCancelAllAgendadoQueueEntries() {
        QueueEntry agendadoEntry = QueueEntry.builder()
                .id(UUID.randomUUID())
                .patient(activePatient)
                .queueStatus(EQueueStatus.AGENDADO)
                .riskColor(ERiskColor.VERDE)
                .registeredAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(activePatient));
        when(queueEntryRepository.findByPatientAndStatusIn(activePatient, List.of(EQueueStatus.AGUARDANDO, EQueueStatus.AGENDADO)))
                .thenReturn(List.of(agendadoEntry));

        useCase.run(patientId);

        verify(queueEntryRepository, times(1)).save(any(QueueEntry.class));
        ArgumentCaptor<QueueEntry> captor = ArgumentCaptor.forClass(QueueEntry.class);
        verify(queueEntryRepository).save(captor.capture());
        
        QueueEntry savedEntry = captor.getValue();
        assertThat(savedEntry.getQueueStatus()).isEqualTo(EQueueStatus.CANCELADO);
    }

    @Test
    @DisplayName("Não deve alterar filas com outros status (CANCELADO, FINALIZADO)")
    void testShouldNotCancelOtherStatuses() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(activePatient));
        when(queueEntryRepository.findByPatientAndStatusIn(activePatient, List.of(EQueueStatus.AGUARDANDO, EQueueStatus.AGENDADO)))
                .thenReturn(List.of());

        useCase.run(patientId);

        verify(queueEntryRepository, times(1)).findByPatientAndStatusIn(
                activePatient,
                List.of(EQueueStatus.AGUARDANDO, EQueueStatus.AGENDADO)
        );

        verify(queueEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve setar o campo ativo como false (inativo) na entity Paciente")
    void testSetAtivoToFalse() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(activePatient));
        when(queueEntryRepository.findByPatientAndStatusIn(activePatient, List.of(EQueueStatus.AGUARDANDO, EQueueStatus.AGENDADO)))
                .thenReturn(List.of());

        Patient result = useCase.run(patientId);

        assertThat(result.isAtivo()).isFalse();
        
        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());
        
        Patient savedPatient = patientCaptor.getValue();
        assertThat(savedPatient.isAtivo()).isFalse();
    }

    @Test
    @DisplayName("Deve preservar o valor dos outros atributos do paciente ao inativar")
    void testPreservePatientFieldsDuringInactivation() {
        
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(activePatient));
        when(queueEntryRepository.findByPatientAndStatusIn(activePatient, List.of(EQueueStatus.AGUARDANDO, EQueueStatus.AGENDADO)))
                .thenReturn(List.of());

        Patient result = useCase.run(patientId);

        assertThat(result.getId()).isEqualTo(patientId);
        assertThat(result.getCpf()).isEqualTo("12345678900");
        assertThat(result.getNome()).isEqualTo("João");
        assertThat(result.getSobrenome()).isEqualTo("Silva");
        assertThat(result.getGender()).isEqualTo(EGender.MASCULINO);
        assertThat(result.getGrupoLegal()).isEqualTo(EPriorityGroup.GERAL);
    }

    @Test
    @DisplayName("Factory method deve criar instância do use case")
    void testFactoryMethodCreatesInstance() {
        InactivatePatient instance = InactivatePatient.create(patientRepository, queueEntryRepository);

        assertThat(instance).isNotNull();
    }

    @Test
    @DisplayName("Deve lidar com múltiplas filas pendentes corretamente")
    void testHandleMultiplePendingEntries() {
        List<QueueEntry> multipleEntries = List.of(
                QueueEntry.builder()
                        .id(UUID.randomUUID())
                        .patient(activePatient)
                        .queueStatus(EQueueStatus.AGUARDANDO)
                        .riskColor(ERiskColor.VERMELHO)
                        .build(),
                QueueEntry.builder()
                        .id(UUID.randomUUID())
                        .patient(activePatient)
                        .queueStatus(EQueueStatus.AGENDADO)
                        .riskColor(ERiskColor.AMARELO)
                        .build(),
                QueueEntry.builder()
                        .id(UUID.randomUUID())
                        .patient(activePatient)
                        .queueStatus(EQueueStatus.AGUARDANDO)
                        .riskColor(ERiskColor.VERDE)
                        .build()
        );

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(activePatient));
        when(queueEntryRepository.findByPatientAndStatusIn(activePatient, List.of(EQueueStatus.AGUARDANDO, EQueueStatus.AGENDADO)))
                .thenReturn(multipleEntries);

        useCase.run(patientId);

        verify(queueEntryRepository, times(3)).save(any(QueueEntry.class));
        verify(patientRepository, times(1)).save(any(Patient.class));
    }
}
