package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.domain.exception.PatientAlreadyRegisteredException;
import br.com.morbus.queueservice.domain.exception.PatientNotEligibleForProcedureException;
import br.com.morbus.queueservice.domain.exception.PatientNotFoundException;
import br.com.morbus.queueservice.domain.exception.PatientInactiveException;
import br.com.morbus.queueservice.domain.exception.ProcedureNotFoundException;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterQueueRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterPatientInQueue Use Case Tests")
class RegisterPatientInQueueUseCaseTest {

    @Mock
    private IPatientRepository patientRepository;

    @Mock
    private IProcedureRepository procedureRepository;

    @Mock
    private IQueueEntryRepository queueEntryRepository;

    @Mock
    private IQueueEventPublisher eventPublisher;

    private RegisterPatientInQueue useCase;
    private UUID patientId;
    private UUID procedureId;
    private Patient patient;
    private Procedure procedure;

    @BeforeEach
    void setUp() {
        useCase = RegisterPatientInQueue.create(patientRepository, procedureRepository, queueEntryRepository, eventPublisher);
        patientId = UUID.randomUUID();
        procedureId = UUID.randomUUID();

        patient = Patient.builder()
                .id(patientId)
                .cpf("12345678900")
                .cns("1234567890123")
                .nome("João")
                .sobrenome("Silva")
                .dataNascimento(LocalDate.of(1980, 5, 15))
                .gender(EGender.MASCULINO)
                .contato("john@email.com")
                .ativo(true)
                .grupoLegal(EPriorityGroup.GERAL)
                .build();

        procedure = Procedure.builder()
                .id(procedureId)
                .noProcedimento("Consulta Cardiologia")
                .idadeMinima(18)
                .idadeMaxima(100)
                .build();
    }

    @Test
    @DisplayName("Registra paciente na fila com sucesso")
    void testExecuteSuccessfully() {
        RegisterQueueRequestDTO dto = new RegisterQueueRequestDTO(patient.getId(), procedureId, ERiskColor.AZUL);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(procedureRepository.findById(procedureId)).thenReturn(Optional.of(procedure));
        when(queueEntryRepository.existsByPatientAndProcedureAndStatusIn(
                eq(patient), eq(procedure), any(List.class)
        )).thenReturn(false);

        QueueEntry result = useCase.execute(dto);

        assertThat(result).isNotNull();
        assertThat(result.getPatient()).isEqualTo(patient);
        assertThat(result.getProcedure()).isEqualTo(procedure);
        assertThat(result.getRiskColor()).isEqualTo(ERiskColor.AZUL);
        assertThat(result.getQueueStatus()).isEqualTo(EQueueStatus.AGUARDANDO);

        verify(patientRepository).save(patient);
        verify(queueEntryRepository).save(any(QueueEntry.class));
        verify(eventPublisher).publishPatientRegistered(any(QueueEntry.class));
    }

    @Test
    @DisplayName("Lança exceção quando DTO é nulo")
    void testExecuteWithNullDTO() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Comando de registro inválido");

        verify(patientRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Lança exceção quando paciente no DTO é nulo")
    void testExecuteWithNullPatient() {
        RegisterQueueRequestDTO dto = new RegisterQueueRequestDTO(null, procedureId, ERiskColor.AZUL);

        assertThatThrownBy(() -> useCase.execute(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Comando de registro inválido");

        verify(patientRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Lança exceção quando procedureId no DTO é nulo")
    void testExecuteWithNullProcedureId() {
        RegisterQueueRequestDTO dto = new RegisterQueueRequestDTO(patient.getId(), null, ERiskColor.AZUL);

        assertThatThrownBy(() -> useCase.execute(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Comando de registro inválido");

        verify(patientRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Lança PatientNotFoundException quando paciente não é encontrado")
    void testExecuteWithPatientNotFound() {
        RegisterQueueRequestDTO dto = new RegisterQueueRequestDTO(patient.getId(), procedureId, ERiskColor.AZUL);

        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(dto))
                .isInstanceOf(PatientNotFoundException.class)
                .hasMessage("Paciente não encontrado");

        verify(procedureRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Lança ProcedureNotFoundException quando procedimento não é encontrado")
    void testExecuteWithProcedureNotFound() {
        RegisterQueueRequestDTO dto = new RegisterQueueRequestDTO(patient.getId(), procedureId, ERiskColor.AZUL);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(procedureRepository.findById(procedureId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(dto))
                .isInstanceOf(ProcedureNotFoundException.class);

        verify(queueEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lança exceção quando paciente é menor que idade mínima")
    void testExecuteWithPatientTooYoung() {
        Patient youngPatient = Patient.builder()
                .id(patientId)
                .cpf("12345678900")
                .cns("1234567890123")
                .nome("João")
                .sobrenome("Silva")
                .dataNascimento(LocalDate.now().minusYears(10))
                .gender(EGender.MASCULINO)
                .contato("john@email.com")
                .ativo(true)
                .grupoLegal(EPriorityGroup.GERAL)
                .build();

        RegisterQueueRequestDTO dto = new RegisterQueueRequestDTO(youngPatient.getId(), procedureId, ERiskColor.AZUL);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(youngPatient));
        when(procedureRepository.findById(procedureId)).thenReturn(Optional.of(procedure));

        assertThatThrownBy(() -> useCase.execute(dto))
                .isInstanceOf(PatientNotEligibleForProcedureException.class)
                .hasMessageContaining("não compatível para o procedimento");

        verify(queueEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lança exceção quando paciente é maior que idade máxima")
    void testExecuteWithPatientTooOld() {
        Patient oldPatient = Patient.builder()
                .id(patientId)
                .cpf("12345678900")
                .cns("1234567890123")
                .nome("João")
                .sobrenome("Silva")
                .dataNascimento(LocalDate.now().minusYears(120))
                .gender(EGender.MASCULINO)
                .contato("john@email.com")
                .ativo(true)
                .grupoLegal(EPriorityGroup.GERAL)
                .build();

        RegisterQueueRequestDTO dto = new RegisterQueueRequestDTO(oldPatient.getId(), procedureId, ERiskColor.AZUL);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(oldPatient));
        when(procedureRepository.findById(procedureId)).thenReturn(Optional.of(procedure));

        assertThatThrownBy(() -> useCase.execute(dto))
                .isInstanceOf(PatientNotEligibleForProcedureException.class)
                .hasMessageContaining("não compatível para o procedimento");

        verify(queueEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lança exceção quando data de nascimento é nula")
    void testExecuteWithNullBirthDate() {
        Patient patientNoBirthDate = Patient.builder()
                .id(patientId)
                .cpf("12345678900")
                .cns("1234567890123")
                .nome("João")
                .sobrenome("Silva")
                .dataNascimento(null)
                .gender(EGender.MASCULINO)
                .contato("john@email.com")
                .ativo(true)
                .grupoLegal(EPriorityGroup.GERAL)
                .build();

        RegisterQueueRequestDTO dto = new RegisterQueueRequestDTO(patientNoBirthDate.getId(), procedureId, ERiskColor.AZUL);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patientNoBirthDate));
        when(procedureRepository.findById(procedureId)).thenReturn(Optional.of(procedure));

        assertThatThrownBy(() -> useCase.execute(dto))
                .isInstanceOf(PatientNotEligibleForProcedureException.class)
                .hasMessage("Data de nascimento do paciente é obrigatória");

        verify(queueEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lança PatientAlreadyRegisteredException quando paciente já está na fila com status AGUARDANDO")
    void testExecuteWithPatientAlreadyRegisteredAguardando() {
        RegisterQueueRequestDTO dto = new RegisterQueueRequestDTO(patient.getId(), procedureId, ERiskColor.AZUL);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(procedureRepository.findById(procedureId)).thenReturn(Optional.of(procedure));
        when(queueEntryRepository.existsByPatientAndProcedureAndStatusIn(
                eq(patient), eq(procedure), any(List.class)
        )).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(dto))
                .isInstanceOf(PatientAlreadyRegisteredException.class)
                .hasMessage("Paciente já está registrado na fila (AGUARDANDO ou AGENDADO) para este procedimento");

        verify(queueEntryRepository, never()).save(any());
        verify(eventPublisher, never()).publishPatientRegistered(any());
    }

    @Test
    @DisplayName("Atualiza grupoLegal do paciente durante registro")
    void testExecuteUpdatesPatientPriorityGroup() {
        Patient patientIdoso = Patient.builder()
                .id(patientId)
                .cpf("12345678900")
                .cns("1234567890123")
                .nome("João")
                .sobrenome("Silva")
                .dataNascimento(LocalDate.of(1960, 5, 15))
                .gender(EGender.MASCULINO)
                .contato("john@email.com")
                .ativo(true)
                .grupoLegal(EPriorityGroup.GERAL)
                .build();

        RegisterQueueRequestDTO dto = new RegisterQueueRequestDTO(patientIdoso.getId(), procedureId, ERiskColor.AZUL);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patientIdoso));
        when(procedureRepository.findById(procedureId)).thenReturn(Optional.of(procedure));
        when(queueEntryRepository.existsByPatientAndProcedureAndStatusIn(
                eq(patientIdoso), eq(procedure), any(List.class)
        )).thenReturn(false);

        useCase.execute(dto);

        verify(patientRepository).save(patientIdoso);
    }

    // Testes para validatePatient

    @Test
    @DisplayName("validatePatient: Lança IllegalArgumentException quando DTO é nulo")
    void testValidatePatientWithNullDTO() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Comando de registro inválido");
    }

    @Test
    @DisplayName("validatePatient: Lança IllegalArgumentException quando paciente é nulo")
    void testValidatePatientWithNullPatientField() {
        RegisterQueueRequestDTO dto = new RegisterQueueRequestDTO(null, procedureId, ERiskColor.AZUL);

        assertThatThrownBy(() -> useCase.execute(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Comando de registro inválido");
    }

    @Test
    @DisplayName("validatePatient: Lança IllegalArgumentException quando procedureId é nulo")
    void testValidatePatientWithNullProcedureIdField() {
        RegisterQueueRequestDTO dto = new RegisterQueueRequestDTO(patient.getId(), null, ERiskColor.AZUL);

        assertThatThrownBy(() -> useCase.execute(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Comando de registro inválido");
    }

    // Testes para validateIfActivePatient

    @Test
    @DisplayName("validateIfActivePatient: Lança PatientInactivatedException quando paciente está inativo")
    void testValidateIfActivePatientInactive() {
        // Criar um paciente que está inativo
        Patient inactivePatient = Patient.builder()
                .id(patientId)
                .cpf("12345678900")
                .cns("1234567890123")
                .nome("João")
                .sobrenome("Silva")
                .dataNascimento(LocalDate.of(1980, 5, 15))
                .gender(EGender.MASCULINO)
                .contato("john@email.com")
                .ativo(false)  // Paciente inativo
                .grupoLegal(EPriorityGroup.GERAL)
                .build();

        RegisterQueueRequestDTO dto = new RegisterQueueRequestDTO(inactivePatient.getId(), procedureId, ERiskColor.AZUL);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(inactivePatient));
        when(procedureRepository.findById(procedureId)).thenReturn(Optional.of(procedure));

        assertThatThrownBy(() -> useCase.execute(dto))
                .isInstanceOf(PatientInactiveException.class)
                .hasMessage("O paciente está inativo");

        verify(patientRepository, never()).save(any());
        verify(queueEntryRepository, never()).save(any());
        verify(eventPublisher, never()).publishPatientRegistered(any());
    }

}
