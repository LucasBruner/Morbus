package br.com.morbus.queueservice.infrastructure.database.repository;

import br.com.morbus.queueservice.infrastructure.database.entity.ProcedureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcedureJpaRepository extends JpaRepository<ProcedureEntity, UUID> {

    @Query("""
        SELECT p
        FROM ProcedureEntity p
        WHERE p.coProcedimento = :coProcedimento
    """)
    Optional<ProcedureEntity> findByCoProcedimento(
            @Param("coProcedimento") String coProcedimento
    );
}
