package br.com.morbus.queueservice.infrastructure.database.repository;

import br.com.morbus.queueservice.infrastructure.database.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PatientJpaRepository extends JpaRepository<PatientEntity, UUID>{

}
