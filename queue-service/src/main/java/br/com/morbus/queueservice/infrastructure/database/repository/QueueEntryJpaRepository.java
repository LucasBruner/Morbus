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

    @Query("""
        SELECT q
        FROM QueueEntryEntity q
        WHERE (:status IS NULL OR q.status = :status)
          AND (:riskColor IS NULL OR q.riskColor = :riskColor)
        ORDER BY
            CASE q.tipoFila WHEN br.com.morbus.queueservice.domain.enums.EDestino.FILA_REGULADA THEN 0 ELSE 1 END ASC,
            q.riskColor ASC,
            q.patient.grupoLegal ASC,
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
            q.patient.grupoLegal ASC,
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
            q.patient.grupoLegal ASC,
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
