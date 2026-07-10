package br.com.morbus.queueservice.domain.repository;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IQueueEntryRepository {
    void save(QueueEntry entry);
    Optional<QueueEntry> findById(UUID id);
    Optional<QueueEntry> findNextByPriority();
    Optional<QueueEntry> findByPatient(Patient patient);
    List<QueueEntry> findAllOrderedByPriority();
    int countEntriesWithHigherPriority(QueueEntry entry); // Conta quantas entradas do mesmo procedimento/status têm prioridade maior (posição = retorno + 1)
    boolean existsByPatientAndProcedureAndStatusIn(Patient patient, Procedure procedure, List<EQueueStatus> statuses);
    List<QueueEntry> findByPatientAndStatusIn(Patient patient, List<EQueueStatus> statuses);
    List<QueueEntry> findByProcedureIdAndFilters(UUID procedureId, EQueueStatus status, ERiskColor riskColor);
}
