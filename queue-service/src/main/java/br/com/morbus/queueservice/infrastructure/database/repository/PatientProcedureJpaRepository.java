package br.com.morbus.queueservice.infrastructure.database.repository;

import br.com.morbus.queueservice.infrastructure.database.entity.PatientProcedureEntity;
import br.com.morbus.queueservice.infrastructure.database.entity.PatientProcedureId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientProcedureJpaRepository
        extends JpaRepository<PatientProcedureEntity, PatientProcedureId> {
}
