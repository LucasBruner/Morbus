package br.com.morbus.queueservice.infrastructure.database.repository;

import br.com.morbus.queueservice.infrastructure.database.entity.UnitProcedureQuotaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnitProcedureQuotaJpaRepository extends JpaRepository<UnitProcedureQuotaEntity, UUID> {

    @Query("""
        SELECT q
        FROM UnitProcedureQuotaEntity q
        WHERE q.unitId = :unitId
        AND q.procedureId = :procedureId
    """)
    Optional<UnitProcedureQuotaEntity> findByUnitIdAndProcedureId(
            @Param("unitId") UUID unitId,
            @Param("procedureId") UUID procedureId
    );
}
