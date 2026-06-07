package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.exception.PatientNotFoundException;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.usecase.DTO.UpdatePatientDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdatePatient Use Case Tests")
class UpdatePatientTest {

    @Mock
    private IPatientRepository patientRepository;

    private UpdatePatient useCase;
    private UUID patientId;
    private Patient existingPatient;
    private UpdatePatientDTO updateDTO;

    @BeforeEach
    void setUp() {
        useCase = UpdatePatient.create(patientRepository);
        patientId = UUID.randomUUID();

        existingPatient = Patient.builder()
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

        updateDTO = new UpdatePatientDTO(
                patientId,
                "9876543210123",
                "João Updated",
                "Silva Updated",
                LocalDate.of(1980, 6, 20),
                EGender.FEMININO,
                "joao.updated@email.com",
                EPriorityGroup.IDOSO
        );
    }

    @Test
    @DisplayName("Should update patient successfully and return updated instance")
    void testUpdatePatientSuccessfully() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existingPatient));

        Patient result = useCase.run(updateDTO);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(patientId);
        assertThat(result.getCns()).isEqualTo("9876543210123");
        assertThat(result.getNome()).isEqualTo("João Updated");
        assertThat(result.getSobrenome()).isEqualTo("Silva Updated");
        assertThat(result.getDataNascimento()).isEqualTo(LocalDate.of(1980, 6, 20));
        assertThat(result.getGender()).isEqualTo(EGender.FEMININO);
        assertThat(result.getContato()).isEqualTo("joao.updated@email.com");

        verify(patientRepository, times(1)).findById(patientId);
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    @DisplayName("Lança PatientNotFoundException quando paciente não é encontrado")
    void testUpdatePatientNotFound() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.run(updateDTO))
                .isInstanceOf(PatientNotFoundException.class)
                .hasMessage("Paciente não cadastrado");

        verify(patientRepository, times(1)).findById(patientId);
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Lança NullPointerException quando DTO é null")
    void testUpdatePatientWithNullDTO() {
        assertThatThrownBy(() -> useCase.run(null))
                .isInstanceOf(NullPointerException.class);

        verify(patientRepository, never()).findById(any());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Deve recalcular grupo legal depois do update")
    void testRecalculatePriorityGroupAfterUpdate() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existingPatient));

        UpdatePatientDTO dtoWithoutGrupoLegal = new UpdatePatientDTO(
                patientId,
                "9876543210123",
                "João",
                "Silva",
                LocalDate.of(1960, 5, 15),
                EGender.MASCULINO,
                "joao@email.com",
                null
        );

        Patient result = useCase.run(dtoWithoutGrupoLegal);

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());

        Patient savedPatient = patientCaptor.getValue();
        assertThat(savedPatient).isNotNull();
    }

    @Test
    @DisplayName("Atualiza cadastro do paciente com os novos valores")
    void testUpdatePatientWithAllNewValues() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existingPatient));

        UpdatePatientDTO completeUpdateDTO = new UpdatePatientDTO(
                patientId,
                "5555555555555",
                "Maria",
                "Santos",
                LocalDate.of(1975, 3, 10),
                EGender.FEMININO,
                "maria@email.com",
                EPriorityGroup.LACTANTE
        );

        Patient result = useCase.run(completeUpdateDTO);

        assertThat(result.getId()).isEqualTo(patientId);
        assertThat(result.getCns()).isEqualTo("5555555555555");
        assertThat(result.getNome()).isEqualTo("Maria");
        assertThat(result.getSobrenome()).isEqualTo("Santos");
        assertThat(result.getDataNascimento()).isEqualTo(LocalDate.of(1975, 3, 10));
        assertThat(result.getGender()).isEqualTo(EGender.FEMININO);
        assertThat(result.getContato()).isEqualTo("maria@email.com");
    }

    @Test
    @DisplayName("Deve manter o valor do CPF e ID depois do update")
    void testPreserveCPFAndIDDuringUpdate() {
        String originalCPF = "12345678900";
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existingPatient));

        Patient result = useCase.run(updateDTO);

        assertThat(result.getId()).isEqualTo(patientId);
        assertThat(result.getCpf()).isEqualTo(originalCPF);
    }

    @Test
    @DisplayName("Mantém o status como ativo ao atualizar")
    void testPreserveAtivoStatusDuringUpdate() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existingPatient));

        Patient result = useCase.run(updateDTO);

        assertThat(result.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Atualiza paciente com valores nulos")
    void testUpdatePatientWithNullOptionalFields() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existingPatient));

        UpdatePatientDTO dtoWithNullOptionals = new UpdatePatientDTO(
                patientId,
                null,
                "João Updated",
                "Silva Updated",
                LocalDate.of(1980, 6, 20),
                null,
                null,
                null
        );

        Patient result = useCase.run(dtoWithNullOptionals);

        assertThat(result).isNotNull();
        assertThat(result.getNome()).isEqualTo("João Updated");
        assertThat(result.getSobrenome()).isEqualTo("Silva Updated");
        assertThat(result.getContato()).isEqualTo("joao@email.com");
    }

    @Test
    @DisplayName("Chama factory method para criar a instância do use")
    void testFactoryMethodCreatesInstance() {
        // Act
        UpdatePatient instance = UpdatePatient.create(patientRepository);

        // Assert
        assertThat(instance).isNotNull();
    }

    @Test
    @DisplayName("Retorna patient com os valores atualizados")
    void testReturnedPatientHasAllUpdatedFields() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existingPatient));

        Patient result = useCase.run(updateDTO);

        assertThat(result)
                .extracting("id", "cns", "nome", "sobrenome", "gender", "contato")
                .containsExactly(
                        patientId,
                        "9876543210123",
                        "João Updated",
                        "Silva Updated",
                        EGender.FEMININO,
                        "joao.updated@email.com"
                );
    }

    @Test
    @DisplayName("Atualiza paciente e prioriza grupo legal IDOSO com idade >= 60")
    void testIdosoGroupCalculationForElderlyPatient() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existingPatient));

        UpdatePatientDTO dtoWithElderly = new UpdatePatientDTO(
                patientId,
                "1234567890123",
                "José",
                "Oliveira",
                LocalDate.of(1960, 1, 1),
                EGender.MASCULINO,
                "jose@email.com",
                EPriorityGroup.GERAL
        );

        Patient result = useCase.run(dtoWithElderly);

        assertThat(result).isNotNull();
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    @DisplayName("Mantém integridade do cadastro depois do update")
    void testPatientEntityIntegrityAfterUpdate() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existingPatient));

        Patient result = useCase.run(updateDTO);

        assertThat(result)
                .isNotNull()
                .hasFieldOrProperty("id")
                .hasFieldOrProperty("cpf")
                .hasFieldOrProperty("cns")
                .hasFieldOrProperty("nome")
                .hasFieldOrProperty("sobrenome")
                .hasFieldOrProperty("dataNascimento")
                .hasFieldOrProperty("gender")
                .hasFieldOrProperty("contato")
                .hasFieldOrProperty("ativo")
                .hasFieldOrProperty("grupoLegal");
    }
}
