package br.com.morbus.queueservice.infrastructure.database.persistence;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.infrastructure.database.entity.PatientEntity;
import br.com.morbus.queueservice.infrastructure.database.entity.ProcedureEntity;
import br.com.morbus.queueservice.infrastructure.database.entity.QueueEntryEntity;
import br.com.morbus.queueservice.infrastructure.database.repository.QueueEntryJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class QueueEntryRepositoryImpl implements IQueueEntryRepository {

    private final QueueEntryJpaRepository repository;
    private final PatientRepositoryImpl patientRepositoryImpl;
    private final ProcedureRepositoryImpl procedureRepositoryImpl;

    public QueueEntryRepositoryImpl(QueueEntryJpaRepository queueEntryJpaRepository, PatientRepositoryImpl patientRepositoryImpl, ProcedureRepositoryImpl procedureRepositoryImpl) {
        this.repository = queueEntryJpaRepository;
        this.patientRepositoryImpl = patientRepositoryImpl;
        this.procedureRepositoryImpl = procedureRepositoryImpl;
    }

    @Override
    public void save(QueueEntry entry) {
        QueueEntryEntity queueEntryEntity = mapToEntityQueue(entry);
        repository.save(queueEntryEntity);
    }

    @Override
    public Optional<QueueEntry> findById(UUID id) {
        Optional<QueueEntryEntity> queueEntity = repository.findById(id);
        return queueEntity.map(this::mapToDomainQueue);
    }

    @Override
    public Optional<QueueEntry> findNextByPriority() {
        List<QueueEntryEntity> entryList = repository.findByPriority(null, null);
        return Optional.ofNullable(mapToDomainQueue(entryList.getFirst()));
    }

    @Override
    public Optional<QueueEntry> findByPatient(Patient patient) {
        List<QueueEntryEntity> entryList = repository.findByPatient(patient.getId());
        return Optional.ofNullable(mapToDomainQueue(entryList.getFirst()));
    }

    @Override
    public List<QueueEntry> findAllOrderedByPriority() {
        List<QueueEntryEntity> entryList = repository.findAllOrderedByPriority(null, null);

        return entryList.stream()
                .map(this::mapToDomainQueue)
                .toList();
    }

    @Override
    public int countEntriesWithHigherPriority(QueueEntry entry) {
        return repository.countEntriesWithHigherPriority(entry.getId());
    }

    @Override
    public boolean existsByPatientAndProcedureAndStatusIn(Patient patient, Procedure procedure, List<EQueueStatus> statuses) {
        return repository
                .existsByPatientAndProcedureAndStatusIn(patient.getId(), procedure.getId(), statuses) > 0;
    }

    @Override
    public List<QueueEntry> findByPatientAndStatusIn(Patient patient, List<EQueueStatus> statuses) {
        List<QueueEntryEntity> entryList = repository.findByPatientAndStatusIn(patient.getId(), statuses);

        return entryList.stream()
                .map(this::mapToDomainQueue)
                .toList();
    }

    @Override
    public List<QueueEntry> findByProcedureIdAndFilters(UUID procedureId, EQueueStatus status, ERiskColor riskColor) {
        List<QueueEntryEntity> entryList = repository
                .findByProcedureIdAndFilters(procedureId, status, riskColor);

        return entryList.stream()
                .map(this::mapToDomainQueue)
                .toList();
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

    private QueueEntryEntity mapToEntityQueue(QueueEntry queueEntry){
        PatientEntity patient = patientRepositoryImpl.mapToEntityPatient(queueEntry.getPatient());
        ProcedureEntity procedure = procedureRepositoryImpl.mapToEntityProcedure(queueEntry.getProcedure());

        return QueueEntryEntity.builder()
                .id(queueEntry.getId())
                .patient(patient)
                .procedure(procedure)
                .riskColor(queueEntry.getRiskColor())
                .status(queueEntry.getQueueStatus())
                .registeredAt(queueEntry.getRegisteredAt())
                .updatedAt(queueEntry.getUpdatedAt())
                .build();
    }
}
