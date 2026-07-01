package br.com.morbus.regulacao.adapters.out.jpa.quota;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface IQuotaJpaRepository extends JpaRepository<QuotaEntity, UUID> {
    Optional<QuotaEntity> findByUnitIdAndProcedureIdAndPeriodStart(UUID unitId, UUID procedureId, LocalDate periodStart);
    @Modifying
    @Query("UPDATE QuotaEntity q SET q.currentCount = q.currentCount +1 " +
            "WHERE q.id = :id AND q.currentCount < q.maxPerPeriod")
    int incrementarSeDisponivel(@Param("id")UUID id);
}
