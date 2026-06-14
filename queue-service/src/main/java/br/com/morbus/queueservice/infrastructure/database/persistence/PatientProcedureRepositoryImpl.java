package br.com.morbus.queueservice.infrastructure.database.persistence;

import br.com.morbus.queueservice.domain.repository.IPatientProcedureRepository;
import br.com.morbus.queueservice.infrastructure.database.entity.PatientProcedureEntity;
import br.com.morbus.queueservice.infrastructure.database.entity.PatientProcedureId;
import br.com.morbus.queueservice.infrastructure.database.repository.PatientProcedureJpaRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PatientProcedureRepositoryImpl implements IPatientProcedureRepository {

    private final PatientProcedureJpaRepository repository;

    public PatientProcedureRepositoryImpl(PatientProcedureJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(UUID patientId, UUID procedureId) {
        PatientProcedureId id = new PatientProcedureId(patientId, procedureId);
        PatientProcedureEntity entity = PatientProcedureEntity.builder()
                .id(id)
                .build();
        repository.save(entity);
    }

    @Override
    public boolean existsByPatientAndProcedure(UUID patientId, UUID procedureId) {
        PatientProcedureId id = new PatientProcedureId(patientId, procedureId);
        return repository.existsById(id);
    }

    @Override
    public void deleteByPatientAndProcedure(UUID patientId, UUID procedureId) {
        PatientProcedureId id = new PatientProcedureId(patientId, procedureId);
        repository.deleteById(id);
    }
}
