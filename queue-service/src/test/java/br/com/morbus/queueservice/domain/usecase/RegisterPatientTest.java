package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.enums.EGender;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.exception.PatientAlreadyExistsException;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterPatientDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterPatient Use Case Tests")
class RegisterPatientTest {

    @Mock
    private IPatientRepository patientRepository;

    private RegisterPatient useCase;
    private RegisterPatientDTO patientDTO;

    @BeforeEach
    void setUp() {
        useCase = RegisterPatient.create(patientRepository);
        patientDTO = new RegisterPatientDTO(
                "12345678900",
                "1234567890123",
                "João",
                "Silva",
                LocalDate.of(1980, 5, 15),
                EGender.MASCULINO,
                "joao@email.com",
                EPriorityGroup.GERAL
        );
    }

    @Test
    @DisplayName("Lança PatientAlreadyExistsException quando CPF já existe")
    void testRunWithExistingCpf() {
        Patient existingPatient = Patient.builder()
                .cpf(patientDTO.cpf())
                .nome("Outro")
                .build();

        when(patientRepository.findByCpf(patientDTO.cpf())).thenReturn(Optional.of(existingPatient));

        assertThatThrownBy(() -> useCase.run(patientDTO))
                .isInstanceOf(PatientAlreadyExistsException.class)
                .hasMessageContaining("já existe");

        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lança PatientAlreadyExistsException quando CNS já existe")
    void testRunWithExistingCns() {
        Patient existingPatient = Patient.builder()
                .cns(patientDTO.cns())
                .nome("Outro")
                .build();

        when(patientRepository.findByCpf(patientDTO.cpf())).thenReturn(Optional.empty());
        when(patientRepository.findByCns(patientDTO.cns())).thenReturn(Optional.of(existingPatient));

        assertThatThrownBy(() -> useCase.run(patientDTO))
                .isInstanceOf(PatientAlreadyExistsException.class)
                .hasMessageContaining("já existe");

        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Retorna Patient quando CPF é único")
    void testRunWithUniqueCpf() {
        when(patientRepository.findByCpf(patientDTO.cpf())).thenReturn(Optional.empty());

        Patient result = useCase.run(patientDTO);

        assertThat(result).isNotNull();
        assertThat(result.getCpf()).isEqualTo(patientDTO.cpf());
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("Classifica paciente >= 60 anos como IDOSO (sobrescreve grupo do DTO)")
    void testRunWithIdosoAge() {
        RegisterPatientDTO idosoDTO = new RegisterPatientDTO(
                "12345678900",
                "1234567890123",
                "Maria",
                "Santos",
                LocalDate.of(1960, 5, 15),  // 64 anos
                EGender.FEMININO,
                "maria@email.com",
                EPriorityGroup.GERAL
        );

        when(patientRepository.findByCpf(idosoDTO.cpf())).thenReturn(Optional.empty());

        Patient result = useCase.run(idosoDTO);

        assertThat(result.getGrupoLegal()).isEqualTo(EPriorityGroup.IDOSO);
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("Usa grupoLegal do DTO quando idade < 60")
    void testRunWithGestanteGroup() {
        RegisterPatientDTO gestanteDTO = new RegisterPatientDTO(
                "12345678900",
                "1234567890123",
                "Ana",
                "Costa",
                LocalDate.of(1990, 5, 15),  // 34 anos
                EGender.FEMININO,
                "ana@email.com",
                EPriorityGroup.GESTANTE
        );

        when(patientRepository.findByCpf(gestanteDTO.cpf())).thenReturn(Optional.empty());

        Patient result = useCase.run(gestanteDTO);

        assertThat(result.getGrupoLegal()).isEqualTo(EPriorityGroup.GESTANTE);
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("Usa GERAL quando grupoLegal do DTO é nulo e idade < 60")
    void testRunWithNullGroupDefaultsToGeral() {
        RegisterPatientDTO dtoWithNullGroup = new RegisterPatientDTO(
                "12345678900",
                "1234567890123",
                "Pedro",
                "Oliveira",
                LocalDate.of(1990, 5, 15),  // 34 anos
                EGender.MASCULINO,
                "pedro@email.com",
                null
        );

        when(patientRepository.findByCpf(dtoWithNullGroup.cpf())).thenReturn(Optional.empty());

        Patient result = useCase.run(dtoWithNullGroup);

        assertThat(result.getGrupoLegal()).isEqualTo(EPriorityGroup.GERAL);
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("Persiste Patient via save() e retorna a entidade criada")
    void testRunPersistsAndReturnsPatient() {
        when(patientRepository.findByCpf(patientDTO.cpf())).thenReturn(Optional.empty());

        Patient result = useCase.run(patientDTO);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getCpf()).isEqualTo(patientDTO.cpf());
        assertThat(result.getNome()).isEqualTo(patientDTO.nome());
        assertThat(result.getSobrenome()).isEqualTo(patientDTO.sobrenome());
        assertThat(result.getDataNascimento()).isEqualTo(patientDTO.dataNascimento());
        assertThat(result.isAtivo()).isTrue();

        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("CA-04: Retorna Patient com ID gerado (UUID)")
    void testRunGeneratesUUID() {
        when(patientRepository.findByCpf(patientDTO.cpf())).thenReturn(Optional.empty());

        Patient result1 = useCase.run(patientDTO);
        Patient result2 = useCase.run(patientDTO);

        assertThat(result1.getId()).isNotNull();
        assertThat(result2.getId()).isNotNull();
        assertThat(result1.getId()).isNotEqualTo(result2.getId());
    }

    @Test
    @DisplayName("Factory create() instancia RegisterPatient corretamente")
    void testFactoryCreateInstantiatesCorrectly() {
        RegisterPatient instance = RegisterPatient.create(patientRepository);

        assertThat(instance).isNotNull();
        assertThat(instance).isInstanceOf(RegisterPatient.class);
    }

    @Test
    @DisplayName("Método run() executa a lógica completa")
    void testMethodRunExecutesLogic() {
        when(patientRepository.findByCpf(patientDTO.cpf())).thenReturn(Optional.empty());

        Patient result = useCase.run(patientDTO);

        assertThat(result).isNotNull();
        assertThat(result.getCpf()).isEqualTo(patientDTO.cpf());
        verify(patientRepository).findByCpf(patientDTO.cpf());
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("Paciente com 60 anos exatos é classificado como IDOSO")
    void testExactly60YearsIsIdoso() {
        LocalDate birthDate = LocalDate.now().minusYears(60);
        RegisterPatientDTO dtoAge60 = new RegisterPatientDTO(
                "12345678900",
                "1234567890123",
                "Roberto",
                "Santos",
                birthDate,
                EGender.MASCULINO,
                "roberto@email.com",
                EPriorityGroup.GERAL
        );

        when(patientRepository.findByCpf(dtoAge60.cpf())).thenReturn(Optional.empty());

        Patient result = useCase.run(dtoAge60);

        assertThat(result.getGrupoLegal()).isEqualTo(EPriorityGroup.IDOSO);
    }

    @Test
    @DisplayName("Paciente com 59 anos não é classificado como IDOSO")
    void testAge59IsNotIdoso() {
        LocalDate birthDate = LocalDate.now().minusYears(59);
        RegisterPatientDTO dtoAge59 = new RegisterPatientDTO(
                "12345678900",
                "1234567890123",
                "Carlos",
                "Dias",
                birthDate,
                EGender.MASCULINO,
                "carlos@email.com",
                EPriorityGroup.GERAL
        );

        when(patientRepository.findByCpf(dtoAge59.cpf())).thenReturn(Optional.empty());

        Patient result = useCase.run(dtoAge59);

        assertThat(result.getGrupoLegal()).isEqualTo(EPriorityGroup.GERAL);
    }

    @Test
    @DisplayName("Paciente sempre é criado com ativo = true")
    void testPatientIsAlwaysCreatedActive() {
        when(patientRepository.findByCpf(patientDTO.cpf())).thenReturn(Optional.empty());

        Patient result = useCase.run(patientDTO);

        assertThat(result.isAtivo()).isTrue();
    }
}
