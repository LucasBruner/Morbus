package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.exception.PatientAgeNotEligibleException;
import br.com.morbus.queueservice.domain.exception.PatientInactiveException;
import br.com.morbus.queueservice.domain.exception.PatientNotFoundException;
import br.com.morbus.queueservice.domain.exception.ProcedureAlreadyAssignedException;
import br.com.morbus.queueservice.domain.exception.ProcedureNotFoundException;
import br.com.morbus.queueservice.domain.repository.IPatientProcedureRepository;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;
import br.com.morbus.queueservice.domain.usecase.dto.IdProcedureAndPatientDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignProcedureToPatient")
class AssignProcedureToPatientTest {

    @Mock
    private IPatientProcedureRepository patientProcedureRepository;
    @Mock
    private IPatientRepository patientRepository;
    @Mock
    private IProcedureRepository procedureRepository;

    private AssignProcedureToPatient useCase;

    private UUID patientId;
    private UUID procedureId;
    private IdProcedureAndPatientDTO dto;
    private Patient activePatient;
    private Procedure eligibleProcedure;

    @BeforeEach
    void setUp() {
        useCase = AssignProcedureToPatient.create(
                patientProcedureRepository, patientRepository, procedureRepository);

        patientId = UUID.randomUUID();
        procedureId = UUID.randomUUID();
        dto = new IdProcedureAndPatientDTO(patientId, procedureId);

        activePatient = Patient.builder()
                .id(patientId)
                .nome("João Silva")
                .cpf("123.456.789-00")
                .dataNascimento(LocalDate.now().minusYears(30))
                .ativo(true)
                .grupoLegal(EPriorityGroup.GERAL)
                .build();

        eligibleProcedure = Procedure.builder()
                .id(procedureId)
                .coProcedimento("0301010072")
                .noProcedimento("CONSULTA MÉDICA EM ATENÇÃO BÁSICA")
                .idadeMinima(0)
                .idadeMaxima(120)
                .grupo("01")
                .build();
    }

    // ── Caminhos de erro ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando o paciente não é encontrado")
    class QuandoPacienteNaoEncontrado {

        @Test
        @DisplayName("deve lançar PatientNotFoundException")
        void deveLancarPatientNotFoundException() {
            when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.run(dto))
                    .isInstanceOf(PatientNotFoundException.class)
                    .hasMessageContaining("Paciente não encontrado");
        }

        @Test
        @DisplayName("não deve consultar o repositório de procedimentos")
        void naoDeveConsultarRepositorioDeProcedimentos() {
            when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.run(dto))
                    .isInstanceOf(PatientNotFoundException.class);

            verify(procedureRepository, never()).findById(procedureId);
        }
    }

    @Nested
    @DisplayName("quando o paciente está inativo")
    class QuandoPacienteInativo {

        @Test
        @DisplayName("deve lançar PatientInactiveException")
        void deveLancarPatientInactiveException() {
            Patient inactivePatient = Patient.builder()
                    .id(patientId)
                    .ativo(false)
                    .dataNascimento(LocalDate.now().minusYears(30))
                    .grupoLegal(EPriorityGroup.GERAL)
                    .build();
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(inactivePatient));

            assertThatThrownBy(() -> useCase.run(dto))
                    .isInstanceOf(PatientInactiveException.class)
                    .hasMessageContaining("ativo");
        }
    }

    @Nested
    @DisplayName("quando o procedimento não é encontrado")
    class QuandoProcedimentoNaoEncontrado {

        @Test
        @DisplayName("deve lançar ProcedureNotFoundException")
        void deveLancarProcedureNotFoundException() {
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(activePatient));
            when(procedureRepository.findById(procedureId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.run(dto))
                    .isInstanceOf(ProcedureNotFoundException.class)
                    .hasMessageContaining(procedureId.toString());
        }
    }

    @Nested
    @DisplayName("quando o paciente está fora da faixa etária do procedimento")
    class QuandoFaixaEtariaInvalida {

        @Test
        @DisplayName("deve lançar PatientAgeNotEligibleException")
        void deveLancarPatientAgeNotEligibleException() {
            Procedure procedimentoPediatrico = Procedure.builder()
                    .id(procedureId)
                    .idadeMinima(0)
                    .idadeMaxima(12)
                    .build();
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(activePatient));
            when(procedureRepository.findById(procedureId)).thenReturn(Optional.of(procedimentoPediatrico));

            assertThatThrownBy(() -> useCase.run(dto))
                    .isInstanceOf(PatientAgeNotEligibleException.class)
                    .hasMessageContaining("idade");
        }

        @Test
        @DisplayName("não deve verificar duplicidade quando idade é inválida")
        void naoDeveVerificarDuplicidadeQuandoIdadeInvalida() {
            Procedure procedimentoPediatrico = Procedure.builder()
                    .id(procedureId)
                    .idadeMinima(0)
                    .idadeMaxima(12)
                    .build();
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(activePatient));
            when(procedureRepository.findById(procedureId)).thenReturn(Optional.of(procedimentoPediatrico));

            assertThatThrownBy(() -> useCase.run(dto))
                    .isInstanceOf(PatientAgeNotEligibleException.class);

            verify(patientProcedureRepository, never())
                    .existsByPatientAndProcedure(patientId, procedureId);
        }
    }

    @Nested
    @DisplayName("quando o procedimento já está atribuído ao paciente")
    class QuandoProcedimentoJaAtribuido {

        @Test
        @DisplayName("deve lançar ProcedureAlreadyAssignedException")
        void deveLancarProcedureAlreadyAssignedException() {
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(activePatient));
            when(procedureRepository.findById(procedureId)).thenReturn(Optional.of(eligibleProcedure));
            when(patientProcedureRepository.existsByPatientAndProcedure(patientId, procedureId))
                    .thenReturn(true);

            assertThatThrownBy(() -> useCase.run(dto))
                    .isInstanceOf(ProcedureAlreadyAssignedException.class)
                    .hasMessageContaining("atribuído");
        }
    }

    // ── Fluxo feliz ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando todos os dados são válidos")
    class QuandoDadosValidos {

        @BeforeEach
        void configuraFluxoFeliz() {
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(activePatient));
            when(procedureRepository.findById(procedureId)).thenReturn(Optional.of(eligibleProcedure));
            when(patientProcedureRepository.existsByPatientAndProcedure(patientId, procedureId))
                    .thenReturn(false);
        }

        @Test
        @DisplayName("deve salvar o vínculo com patientId antes de procedureId")
        void deveSalvarVinculoComOrdemCorretaDeIds() {
            useCase.run(dto);

            verify(patientProcedureRepository).save(patientId, procedureId);
        }

        @Test
        @DisplayName("não deve salvar com os IDs na ordem invertida")
        void naoDeveSalvarComIdsInvertidos() {
            useCase.run(dto);

            verify(patientProcedureRepository, never()).save(procedureId, patientId);
        }

        @Test
        @DisplayName("deve retornar o procedimento atribuído")
        void deveRetornarProcedimento() {
            Procedure result = useCase.run(dto);

            assertThat(result).isEqualTo(eligibleProcedure);
        }
    }
}
