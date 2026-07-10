package br.com.morbus.queueservice.infrastructure.database.persistence;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EDestino;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.infrastructure.database.entity.PatientEntity;
import br.com.morbus.queueservice.infrastructure.database.entity.ProcedureEntity;
import br.com.morbus.queueservice.infrastructure.database.entity.QueueEntryEntity;
import br.com.morbus.queueservice.infrastructure.database.repository.PatientJpaRepository;
import br.com.morbus.queueservice.infrastructure.database.repository.ProcedureJpaRepository;
import br.com.morbus.queueservice.infrastructure.database.repository.QueueEntryJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class QueueEntryRepositoryImpl implements IQueueEntryRepository {

    private final QueueEntryJpaRepository repository;
    private final PatientJpaRepository patientJpaRepository;
    private final ProcedureJpaRepository procedureJpaRepository;
    private final PatientRepositoryImpl patientRepositoryImpl;
    private final ProcedureRepositoryImpl procedureRepositoryImpl;

    public QueueEntryRepositoryImpl(
            QueueEntryJpaRepository queueEntryJpaRepository,
            PatientJpaRepository patientJpaRepository,
            ProcedureJpaRepository procedureJpaRepository,
            PatientRepositoryImpl patientRepositoryImpl,
            ProcedureRepositoryImpl procedureRepositoryImpl) {
        this.repository = queueEntryJpaRepository;
        this.patientJpaRepository = patientJpaRepository;
        this.procedureJpaRepository = procedureJpaRepository;
        this.patientRepositoryImpl = patientRepositoryImpl;
        this.procedureRepositoryImpl = procedureRepositoryImpl;
    }

    @Override
    public void save(QueueEntry entry) {
        repository.findById(entry.getId()).ifPresentOrElse(
                existing -> {
                    existing.setRiskColor(entry.getRiskColor());
                    existing.setStatus(entry.getQueueStatus());
                    existing.setUpdatedAt(entry.getUpdatedAt());
                },
                () -> {
                    PatientEntity patientRef = patientJpaRepository.getReferenceById(entry.getPatient().getId());
                    ProcedureEntity procedureRef = procedureJpaRepository.getReferenceById(entry.getProcedure().getId());
                    QueueEntryEntity entity = QueueEntryEntity.builder()
                            .id(entry.getId())
                            .patient(patientRef)
                            .procedure(procedureRef)
                            .riskColor(entry.getRiskColor())
                            .tipoFila(entry.getTipoFila() != null ? entry.getTipoFila() : EDestino.FILA_REGULADA)
                            .status(entry.getQueueStatus())
                            .registeredAt(entry.getRegisteredAt())
                            .updatedAt(entry.getUpdatedAt())
                            .solicitacaoId(entry.getSolicitacaoId())
                            .preferredUnitId(entry.getPreferredUnitId())
                            .priorityGroup(entry.getPriorityGroup())
                            .build();
                    repository.save(entity);
                }
        );
    }

    @Override
    public Optional<QueueEntry> findById(UUID id) {
        Optional<QueueEntryEntity> queueEntity = repository.findById(id);
        return queueEntity.map(this::mapToDomainQueue);
    }

    @Override
    public Optional<QueueEntry> findNextByPriority() {
        List<QueueEntryEntity> entryList = repository.findByPriority(EQueueStatus.AGUARDANDO, null);
        return entryList.isEmpty()
                ? Optional.empty()
                : Optional.of(mapToDomainQueue(entryList.getFirst()));
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
        List<QueueEntryEntity> orderedEntries = repository.findByProcedureIdAndFilters(
                entry.getProcedure().getId(), entry.getQueueStatus(), null);

        int count = 0;
        for (QueueEntryEntity candidate : orderedEntries) {
            if (candidate.getId().equals(entry.getId())) {
                break;
            }
            count++;
        }
        return count;
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
                .tipoFila(entity.getTipoFila())
                .queueStatus(entity.getStatus())
                .registeredAt(entity.getRegisteredAt())
                .updatedAt(entity.getUpdatedAt())
                .solicitacaoId(entity.getSolicitacaoId())
                .preferredUnitId(entity.getPreferredUnitId())
                .priorityGroup(entity.getPriorityGroup())
                .build();
    }
}
