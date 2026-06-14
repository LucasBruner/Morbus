package br.com.morbus.queueservice.infrastructure.database.repository;

import br.com.morbus.queueservice.infrastructure.database.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientJpaRepository extends JpaRepository<PatientEntity, UUID> {

    @Query("""
        SELECT p
        FROM PatientEntity p
        WHERE p.cpf = :cpf
    """)
    Optional<PatientEntity> findByCpf(@Param("cpf") String cpf);

    @Query("""
        SELECT p
        FROM PatientEntity p
        WHERE p.cns = :cns
    """)
    Optional<PatientEntity> findByCns(@Param("cns") String cns);

}
