package br.com.morbus.agendamento.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
              AND s.reservados < s.capacidade
            ORDER BY s.dataHora ASC
            """)
    Optional<SlotEntity> findAvailableSlotForProcedureAndUnit(@Param("procedureId") UUID procedureId,
                                                             @Param("preferredUnitId") UUID preferredUnitId);
}
