package br.com.morbus.queueservice.infrastructure.database.repository;

import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.infrastructure.database.entity.QueueEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface QueueEntryJpaRepository extends JpaRepository<QueueEntryEntity, UUID> {

    /**
     * CASE que calcula o grupo de prioridade efetivo em SQL, espelhando
     * {@code PriorityCalculator.getPriorityGroup()}: paciente com 60+ anos
     * (idade recalculada a cada consulta, não o snapshot em patients.grupo_legal)
     * conta como IDOSO (ordinal 0) mesmo que o cadastro não tenha sido atualizado
     * desde que ele completou 60 anos. Os valores 0..5 replicam o ordinal de
     * EPriorityGroup (IDOSO=0, GESTANTE=1, DEFICIENTE=2, LACTANTE=3, OBESO=4, GERAL=5).
     */
    String EFFECTIVE_PRIORITY_GROUP =
            "CASE WHEN (" +
            "YEAR(CURRENT_DATE) - YEAR(q.patient.dataNascimento) - " +
            "CASE WHEN (MONTH(CURRENT_DATE) < MONTH(q.patient.dataNascimento)) " +
            "OR (MONTH(CURRENT_DATE) = MONTH(q.patient.dataNascimento) AND DAY(CURRENT_DATE) < DAY(q.patient.dataNascimento)) " +
            "THEN 1 ELSE 0 END" +
            ") >= 60 THEN 0 ELSE " +
            "CASE q.patient.grupoLegal " +
            "WHEN br.com.morbus.queueservice.domain.enums.EPriorityGroup.IDOSO THEN 0 " +
            "WHEN br.com.morbus.queueservice.domain.enums.EPriorityGroup.GESTANTE THEN 1 " +
            "WHEN br.com.morbus.queueservice.domain.enums.EPriorityGroup.DEFICIENTE THEN 2 " +
            "WHEN br.com.morbus.queueservice.domain.enums.EPriorityGroup.LACTANTE THEN 3 " +
            "WHEN br.com.morbus.queueservice.domain.enums.EPriorityGroup.OBESO THEN 4 " +
            "ELSE 5 END END";

    @Query("""
        SELECT q
        FROM QueueEntryEntity q
        WHERE (:status IS NULL OR q.status = :status)
          AND (:riskColor IS NULL OR q.riskColor = :riskColor)
        ORDER BY
            CASE q.tipoFila WHEN br.com.morbus.queueservice.domain.enums.EDestino.FILA_REGULADA THEN 0 ELSE 1 END ASC,
            q.riskColor ASC,
            """ + EFFECTIVE_PRIORITY_GROUP + """
             ASC,
            q.registeredAt ASC
    """)
    List<QueueEntryEntity> findByPriority(
            @Param("status") EQueueStatus status,
            @Param("riskColor") ERiskColor riskColor
    );

    @Query("""
        SELECT q
        FROM QueueEntryEntity q
        WHERE q.status = 'AGUARDANDO'
        ORDER BY
            CASE q.tipoFila WHEN br.com.morbus.queueservice.domain.enums.EDestino.FILA_REGULADA THEN 0 ELSE 1 END ASC,
            q.riskColor ASC,
            """ + EFFECTIVE_PRIORITY_GROUP + """
             ASC,
            q.registeredAt ASC
    """)
    List<QueueEntryEntity> findAllOrderedByPriority(
            @Param("status") EQueueStatus status,
            @Param("riskColor") ERiskColor riskColor
    );

    @Query("""
        SELECT q
        FROM QueueEntryEntity q
        WHERE q.patient.id = :patientId
        AND q.status IN :statuses
        ORDER BY
            q.riskColor ASC,
            q.patient.grupoLegal ASC,
            q.registeredAt ASC
    """)
    List<QueueEntryEntity> findByPatientAndStatusIn(
            @Param("patientId") UUID patientId,
            @Param("statuses") List<EQueueStatus> statuses
    );

    @Query("""
        SELECT q
        FROM QueueEntryEntity q
        WHERE q.patient.id = :patientId
        ORDER BY
            q.riskColor ASC,
            q.patient.grupoLegal ASC,
            q.registeredAt ASC
    """)
    List<QueueEntryEntity> findByPatient(
            @Param("patientId") UUID patientId
    );

    @Query("""
        SELECT count(q)
        FROM QueueEntryEntity q
        WHERE q.patient.id = :patientId
        AND q.procedure.id = :procedureId
        AND q.status IN :statuses
    """)
    Integer existsByPatientAndProcedureAndStatusIn(
            @Param("patientId") UUID patientId,
            @Param("procedureId") UUID procedureId,
            @Param("statuses") List<EQueueStatus> statuses
    );

    @Query("""
        SELECT q
        FROM QueueEntryEntity q
        WHERE q.procedure.id = :procedureId
        AND q.status = :status
        AND (:riskColor IS NULL OR q.riskColor = :riskColor)
        ORDER BY
            CASE q.tipoFila WHEN br.com.morbus.queueservice.domain.enums.EDestino.FILA_REGULADA THEN 0 ELSE 1 END ASC,
            q.riskColor ASC,
            """ + EFFECTIVE_PRIORITY_GROUP + """
             ASC,
            q.registeredAt ASC
    """)
    List<QueueEntryEntity> findByProcedureIdAndFilters(
            @Param("procedureId") UUID procedureId,
            @Param("status") EQueueStatus status,
            @Param("riskColor") ERiskColor riskColor
    );

    @Query("""
        SELECT COUNT(q)
        FROM QueueEntryEntity q
        WHERE q.preferredUnitId = :unitId
        AND q.procedure.id = :procedureId
        AND q.tipoFila = br.com.morbus.queueservice.domain.enums.EDestino.FILA_ESPERA
        AND q.status = 'AGUARDANDO'
        AND q.registeredAt >= :from
        AND q.registeredAt < :to
    """)
    int countActiveFilaEsperaEntries(
            @Param("unitId") UUID unitId,
            @Param("procedureId") UUID procedureId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
