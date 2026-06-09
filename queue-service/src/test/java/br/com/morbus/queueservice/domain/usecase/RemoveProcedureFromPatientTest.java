package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.exception.ActiveQueueEntriesExistException;
import br.com.morbus.queueservice.domain.exception.PatientNotFoundException;
import br.com.morbus.queueservice.domain.exception.ProcedureNotAssignedException;
import br.com.morbus.queueservice.domain.exception.ProcedureNotFoundException;
import br.com.morbus.queueservice.domain.repository.IPatientProcedureRepository;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.usecase.dto.IdProcedureAndPatientDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RemoveProcedureFromPatient")
class RemoveProcedureFromPatientTest {

    @Mock
    private IPatientProcedureRepository patientProcedureRepository;
    @Mock
    private IQueueEntryRepository queueEntryRepository;
    @Mock
    private IPatientRepository patientRepository;
    @Mock
    private IProcedureRepository procedureRepository;

    private RemoveProcedureFromPatient useCase;

    private UUID patientId;
    private UUID procedureId;
    private IdProcedureAndPatientDTO dto;
    private Patient patient;
    private Procedure procedure;

    @BeforeEach
    void setUp() {
        useCase = RemoveProcedureFromPatient.create(
                patientProcedureRepository, queueEntryRepository, patientRepository, procedureRepository);

        patientId = UUID.randomUUID();
        procedureId = UUID.randomUUID();
        dto = new IdProcedureAndPatientDTO(patientId, procedureId);

        patient = Patient.builder()
                .id(patientId)
                .nome("Maria Silva")
                .dataNascimento(LocalDate.now().minusYears(45))
                .grupoLegal(EPriorityGroup.GERAL)
                .ativo(true)
                .build();

        procedure = Procedure.builder()
                .id(procedureId)
                .coProcedimento("0301010072")
                .noProcedimento("CONSULTA MÉDICA EM ATENÇÃO BÁSICA")
                .idadeMinima(0)
                .idadeMaxima(120)
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
        @DisplayName("não deve verificar vínculo quando o paciente não existe")
        void naoDeveVerificarVinculoQuandoPacienteNaoExiste() {
            when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.run(dto))
                    .isInstanceOf(PatientNotFoundException.class);

            verify(patientProcedureRepository, never())
                    .existsByPatientAndProcedure(any(), any());
        }
    }

    @Nested
    @DisplayName("quando o procedimento não está atribuído ao paciente")
    class QuandoProcedimentoNaoAtribuido {

        @Test
        @DisplayName("deve lançar ProcedureNotAssignedException")
        void deveLancarProcedureNotAssignedException() {
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(patientProcedureRepository.existsByPatientAndProcedure(patientId, procedureId))
                    .thenReturn(false);

            assertThatThrownBy(() -> useCase.run(dto))
                    .isInstanceOf(ProcedureNotAssignedException.class)
                    .hasMessageContaining("não atribuído");
        }
    }

    @Nested
    @DisplayName("quando o procedimento não é encontrado no repositório")
    class QuandoProcedimentoNaoEncontrado {

        @Test
        @DisplayName("deve lançar ProcedureNotFoundException")
        void deveLancarProcedureNotFoundException() {
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(patientProcedureRepository.existsByPatientAndProcedure(patientId, procedureId))
                    .thenReturn(true);
            when(procedureRepository.findById(procedureId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.run(dto))
                    .isInstanceOf(ProcedureNotFoundException.class)
                    .hasMessageContaining(procedureId.toString());
        }
    }

    @Nested
    @DisplayName("quando existem entradas ativas na fila para o procedimento")
    class QuandoExisteEntradaAtivaFila {

        @Test
        @DisplayName("deve lançar ActiveQueueEntriesExistException")
        void deveLancarActiveQueueEntriesExistException() {
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(patientProcedureRepository.existsByPatientAndProcedure(patientId, procedureId))
                    .thenReturn(true);
            when(procedureRepository.findById(procedureId)).thenReturn(Optional.of(procedure));
            when(queueEntryRepository.existsByPatientAndProcedureAndStatusIn(
                    eq(patient), eq(procedure), anyStatusList()))
                    .thenReturn(true);

            assertThatThrownBy(() -> useCase.run(dto))
                    .isInstanceOf(ActiveQueueEntriesExistException.class)
                    .hasMessageContaining("agendado ou aguardando");
        }

        @Test
        @DisplayName("não deve remover o vínculo quando há entradas ativas")
        void naoDeveRemoverVinculoQuandoHaEntradasAtivas() {
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(patientProcedureRepository.existsByPatientAndProcedure(patientId, procedureId))
                    .thenReturn(true);
            when(procedureRepository.findById(procedureId)).thenReturn(Optional.of(procedure));
            when(queueEntryRepository.existsByPatientAndProcedureAndStatusIn(
                    eq(patient), eq(procedure), anyStatusList()))
                    .thenReturn(true);

            assertThatThrownBy(() -> useCase.run(dto))
                    .isInstanceOf(ActiveQueueEntriesExistException.class);

            verify(patientProcedureRepository, never())
                    .deleteByPatientAndProcedure(patientId, procedureId);
        }
    }

    // ── Fluxo feliz ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando todos os dados são válidos")
    class QuandoDadosValidos {

        @BeforeEach
        void configuraFluxoFeliz() {
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(patientProcedureRepository.existsByPatientAndProcedure(patientId, procedureId))
                    .thenReturn(true);
            when(procedureRepository.findById(procedureId)).thenReturn(Optional.of(procedure));
            when(queueEntryRepository.existsByPatientAndProcedureAndStatusIn(
                    eq(patient), eq(procedure), anyStatusList()))
                    .thenReturn(false);
        }

        @Test
        @DisplayName("deve remover o vínculo entre paciente e procedimento")
        void deveRemoverVinculo() {
            useCase.run(dto);

            verify(patientProcedureRepository).deleteByPatientAndProcedure(patientId, procedureId);
        }

        @Test
        @DisplayName("não deve recadastrar o vínculo após a remoção")
        void naoDeveRecadastrarVinculoAposRemocao() {
            useCase.run(dto);

            verify(patientProcedureRepository, never()).save(any(), any());
        }

        @Test
        @DisplayName("deve verificar os status AGUARDANDO e AGENDADO na checagem de fila")
        void deveVerificarStatusAguardandoEAgendado() {
            useCase.run(dto);

            verify(queueEntryRepository).existsByPatientAndProcedureAndStatusIn(
                    eq(patient),
                    eq(procedure),
                    argThat(statuses -> statuses.contains(EQueueStatus.AGUARDANDO)
                            && statuses.contains(EQueueStatus.AGENDADO)
                            && statuses.size() == 2));
        }

        @Test
        @DisplayName("deve executar sem lançar exceção")
        void deveExecutarSemExcecao() {
            assertThatCode(() -> useCase.run(dto)).doesNotThrowAnyException();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static List<EQueueStatus> anyStatusList() {
        return argThat(list -> list != null && !list.isEmpty());
    }
}
