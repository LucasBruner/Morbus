package br.com.morbus.queueservice.infrastructure.database.persistence;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.infrastructure.database.entity.QueueEntryEntity;
import br.com.morbus.queueservice.infrastructure.database.repository.QueueEntryJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class QueueEntryRepositoryImpl implements IQueueEntryRepository {

    private final QueueEntryJpaRepository queueEntryJpaRepository;
    private final PatientRepositoryImpl patientRepositoryImpl;
    private final ProcedureRepositoryImpl procedureRepositoryImpl;

    public QueueEntryRepositoryImpl(QueueEntryJpaRepository queueEntryJpaRepository, PatientRepositoryImpl patientRepositoryImpl, ProcedureRepositoryImpl procedureRepositoryImpl) {
        this.queueEntryJpaRepository = queueEntryJpaRepository;
        this.patientRepositoryImpl = patientRepositoryImpl;
        this.procedureRepositoryImpl = procedureRepositoryImpl;
    }

    @Override
    public void save(QueueEntry entry) {

    }

    @Override
    public Optional<QueueEntry> findById(UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<QueueEntry> findNextByPriority() {
        return Optional.empty();
    }

    @Override
    public Optional<QueueEntry> findByPatient(Patient patient) {
        return Optional.empty();
    }

    @Override
    public List<QueueEntry> findAllOrderedByPriority() {
        return List.of();
    }

    @Override
    public int countEntriesWithHigherPriority(QueueEntry entry) {
        return 0;
    }

    @Override
    public boolean existsByPatientAndProcedureAndStatusIn(Patient patient, Procedure procedure, List<EQueueStatus> statuses) {
        return false;
    }

    @Override
    public List<QueueEntry> findByPatientAndStatusIn(Patient patient, List<EQueueStatus> statuses) {
        return List.of();
    }

    @Override
    public List<QueueEntry> findByProcedureIdAndFilters(UUID procedureId, EQueueStatus status, ERiskColor riskColor) {
        return List.of();
    }

    private QueueEntry mapToDomainQueue(QueueEntryEntity entity){
        Patient patient = patientRepositoryImpl.mapToDomainPatient(entity.getPatient());
        Procedure procedure = procedureRepositoryImpl.mapToDomainProcedure(entity.getProcedure());

        return QueueEntry.builder()
                .id(entity.getId())
                .patient(patient)
                .procedure(procedure)
                .riskColor(entity.getRiskColor())
                .queueStatus(entity.getStatus())
                .registeredAt(entity.getRegisteredAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
