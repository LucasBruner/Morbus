package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface IAgendamentoJpaRepository extends JpaRepository<AgendamentoEntity, UUID> {

    boolean existsByPacienteIdAndSlotId(UUID pacienteId, UUID slotId);

    List<AgendamentoEntity> findAllByStatusAndExpiresAtBefore(EStatusAgendamento status, LocalDateTime now);

    @Query(value = """
            SELECT new br.com.morbus.agendamento.adapter.out.persistence.AgendamentoListProjection(a, s, sch, hu, p)
            FROM AgendamentoEntity a
            JOIN SlotEntity s ON s.id = a.slotId
            JOIN ScheduleEntity sch ON sch.id = s.scheduleId
            JOIN HealthUnitEntity hu ON hu.id = sch.unitId
            LEFT JOIN ProviderEntity p ON p.id = sch.providerId
            WHERE a.pacienteId = :pacienteId
            AND sch.unitId = COALESCE(:unitId, sch.unitId)
            AND a.status = COALESCE(:status, a.status)
            AND s.dataHora >= :dateFrom
            AND s.dataHora <= :dateTo
            ORDER BY a.createdAt DESC
            """)
    List<AgendamentoListProjection> findByPatientAndStatusAndDate(  @Param("pacienteId") UUID pacienteId,
                                                                    @Param("unitId") UUID unitId,
                                                                    @Param("status") EStatusAgendamento status,
                                                                    @Param("dateFrom") LocalDateTime dateFrom,
                                                                    @Param("dateTo") LocalDateTime dateTo);
}
