package br.com.morbus.queueservice.infrastructure.database.persistence;

import br.com.morbus.queueservice.infrastructure.database.entity.PatientProcedureEntity;
import br.com.morbus.queueservice.infrastructure.database.entity.PatientProcedureId;
import br.com.morbus.queueservice.infrastructure.database.repository.PatientProcedureJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatientProcedureRepositoryImpl")
class PatientProcedureRepositoryImplTest {

    @Mock
    private PatientProcedureJpaRepository jpaRepository;

    private PatientProcedureRepositoryImpl repositoryImpl;

    private UUID patientId;
    private UUID procedureId;

    @BeforeEach
    void setUp() {
        repositoryImpl = new PatientProcedureRepositoryImpl(jpaRepository);
        patientId = UUID.randomUUID();
        procedureId = UUID.randomUUID();
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("deve persistir uma entidade com o patientId correto")
        void devePersistirEntidadeComPatientIdCorreto() {
            repositoryImpl.save(patientId, procedureId);

            ArgumentCaptor<PatientProcedureEntity> captor =
                    ArgumentCaptor.forClass(PatientProcedureEntity.class);
            verify(jpaRepository).save(captor.capture());

            assertThat(captor.getValue().getId().getPatientId()).isEqualTo(patientId);
        }

        @Test
        @DisplayName("deve persistir uma entidade com o procedureId correto")
        void devePersistirEntidadeComProcedureIdCorreto() {
            repositoryImpl.save(patientId, procedureId);

            ArgumentCaptor<PatientProcedureEntity> captor =
                    ArgumentCaptor.forClass(PatientProcedureEntity.class);
            verify(jpaRepository).save(captor.capture());

            assertThat(captor.getValue().getId().getProcedureId()).isEqualTo(procedureId);
        }

        @Test
        @DisplayName("não deve inverter a ordem dos IDs ao montar a chave composta")
        void naoDeveInverterdOrdemDosIds() {
            repositoryImpl.save(patientId, procedureId);

            ArgumentCaptor<PatientProcedureEntity> captor =
                    ArgumentCaptor.forClass(PatientProcedureEntity.class);
            verify(jpaRepository).save(captor.capture());

            PatientProcedureId id = captor.getValue().getId();
            assertThat(id.getPatientId()).isNotEqualTo(id.getProcedureId());
            assertThat(id.getPatientId()).isEqualTo(patientId);
            assertThat(id.getProcedureId()).isEqualTo(procedureId);
        }

        @Test
        @DisplayName("deve delegar exatamente uma chamada ao repositório JPA")
        void deveDelegarUmaChamadaAoJpa() {
            repositoryImpl.save(patientId, procedureId);

            verify(jpaRepository).save(any(PatientProcedureEntity.class));
        }
    }

    // ── existsByPatientAndProcedure ───────────────────────────────────────────

    @Nested
    @DisplayName("existsByPatientAndProcedure")
    class ExistsByPatientAndProcedure {

        @Test
        @DisplayName("deve retornar true quando o JPA reportar existência")
        void deveRetornarTrueQuandoExiste() {
            PatientProcedureId expectedId = new PatientProcedureId(patientId, procedureId);
            when(jpaRepository.existsById(expectedId)).thenReturn(true);

            boolean result = repositoryImpl.existsByPatientAndProcedure(patientId, procedureId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("deve retornar false quando o JPA reportar ausência")
        void deveRetornarFalseQuandoNaoExiste() {
            PatientProcedureId expectedId = new PatientProcedureId(patientId, procedureId);
            when(jpaRepository.existsById(expectedId)).thenReturn(false);

            boolean result = repositoryImpl.existsByPatientAndProcedure(patientId, procedureId);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("deve consultar com a chave composta montada na ordem correta")
        void deveConsultarComChaveNaOrdemCorreta() {
            ArgumentCaptor<PatientProcedureId> captor =
                    ArgumentCaptor.forClass(PatientProcedureId.class);
            when(jpaRepository.existsById(any())).thenReturn(false);

            repositoryImpl.existsByPatientAndProcedure(patientId, procedureId);

            verify(jpaRepository).existsById(captor.capture());
            assertThat(captor.getValue().getPatientId()).isEqualTo(patientId);
            assertThat(captor.getValue().getProcedureId()).isEqualTo(procedureId);
        }

        @Test
        @DisplayName("IDs distintos não devem colidir na consulta")
        void idsDistintosNaoDevemColidir() {
            UUID outroPatientId = UUID.randomUUID();
            UUID outroProcedureId = UUID.randomUUID();

            PatientProcedureId idOriginal = new PatientProcedureId(patientId, procedureId);
            PatientProcedureId idAlternativo = new PatientProcedureId(outroPatientId, outroProcedureId);

            when(jpaRepository.existsById(idOriginal)).thenReturn(true);
            when(jpaRepository.existsById(idAlternativo)).thenReturn(false);

            assertThat(repositoryImpl.existsByPatientAndProcedure(patientId, procedureId)).isTrue();
            assertThat(repositoryImpl.existsByPatientAndProcedure(outroPatientId, outroProcedureId)).isFalse();
        }
    }

    // ── deleteByPatientAndProcedure ───────────────────────────────────────────

    @Nested
    @DisplayName("deleteByPatientAndProcedure")
    class DeleteByPatientAndProcedure {

        @Test
        @DisplayName("deve deletar usando a chave composta com patientId correto")
        void deveDeletarComPatientIdCorreto() {
            ArgumentCaptor<PatientProcedureId> captor =
                    ArgumentCaptor.forClass(PatientProcedureId.class);

            repositoryImpl.deleteByPatientAndProcedure(patientId, procedureId);

            verify(jpaRepository).deleteById(captor.capture());
            assertThat(captor.getValue().getPatientId()).isEqualTo(patientId);
        }

        @Test
        @DisplayName("deve deletar usando a chave composta com procedureId correto")
        void deveDeletarComProcedureIdCorreto() {
            ArgumentCaptor<PatientProcedureId> captor =
                    ArgumentCaptor.forClass(PatientProcedureId.class);

            repositoryImpl.deleteByPatientAndProcedure(patientId, procedureId);

            verify(jpaRepository).deleteById(captor.capture());
            assertThat(captor.getValue().getProcedureId()).isEqualTo(procedureId);
        }

        @Test
        @DisplayName("deve delegar exatamente uma chamada ao repositório JPA")
        void deveDelegarUmaChamadaAoJpa() {
            repositoryImpl.deleteByPatientAndProcedure(patientId, procedureId);

            verify(jpaRepository).deleteById(any(PatientProcedureId.class));
        }

        @Test
        @DisplayName("não deve chamar save durante a deleção")
        void naoDeveChamarSaveDuranteDeleção() {
            repositoryImpl.deleteByPatientAndProcedure(patientId, procedureId);

            verify(jpaRepository, never()).save(any());
        }
    }
}
