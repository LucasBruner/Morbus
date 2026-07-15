package br.com.morbus.agendamento.adapter.out.persistence;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ISlotJpaRepository extends JpaRepository<SlotEntity, UUID> {

    @Query(value = """
            SELECT s
            FROM SlotEntity s
            JOIN ScheduleEntity sch ON sch.id = s.scheduleId
            WHERE sch.procedureId = :procedureId
            AND sch.unitId = COALESCE(:preferredUnitId, sch.unitId)
            AND s.status = 'DISPONIVEL'
            ORDER BY s.dataHora ASC
            """)
    List<SlotEntity> findAvailableSlotsForProcedureAndUnit(@Param("procedureId") UUID procedureId,
                                                            @Param("preferredUnitId") UUID preferredUnitId,
                                                            Limit limit);

    default Optional<SlotEntity> findAvailableSlotForProcedureAndUnit(UUID procedureId, UUID preferredUnitId) {
        List<SlotEntity> slots = findAvailableSlotsForProcedureAndUnit(procedureId, preferredUnitId, Limit.of(1));
        return slots.isEmpty() ? Optional.empty() : Optional.of(slots.get(0));
    }

    @Query(value = """
            SELECT s
            FROM SlotEntity s
            JOIN ScheduleEntity sch ON sch.id = s.scheduleId
            WHERE sch.procedureId = :procedureId
            AND sch.unitId = COALESCE(:unitId, sch.unitId)
            AND s.status = 'DISPONIVEL'
            AND s.dataHora >= :dateFrom
            AND s.dataHora <= :dateTo
            ORDER BY s.dataHora ASC
            """)
    List<SlotEntity> findByProcedureAndUnitAndDate( @Param("procedureId") UUID procedureId,
                                                    @Param("unitId") UUID unitId,
                                                    @Param("dateFrom") LocalDateTime dateFrom,
                                                    @Param("dateTo") LocalDateTime dateTo);

    @Query(value = """
            SELECT s
            FROM SlotEntity s
            WHERE s.scheduleId = :scheduleId
            """)
    List<SlotEntity> findByScheduleId(@Param("scheduleId") UUID scheduleId);
}
