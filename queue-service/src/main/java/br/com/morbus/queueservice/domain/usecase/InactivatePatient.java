package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.exception.PatientAlreadyInactiveException;
import br.com.morbus.queueservice.domain.exception.PatientHasActiveQueueEntriesException;
import br.com.morbus.queueservice.domain.exception.PatientNotFoundException;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;

import java.util.List;
import java.util.UUID;

public class InactivatePatient {

    private final IPatientRepository patientRepository;
    private final IQueueEntryRepository queueEntryRepository;

    private InactivatePatient(IPatientRepository patientRepository, IQueueEntryRepository queueEntryRepository) {
        this.patientRepository = patientRepository;
        this.queueEntryRepository = queueEntryRepository;
    }

    public static InactivatePatient create(IPatientRepository patientRepository, IQueueEntryRepository queueEntryRepository) {
        return new InactivatePatient(patientRepository, queueEntryRepository);
    }

    public Patient run(UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Paciente não cadastrado"));

        if (!patient.isAtivo()) {
            throw new PatientAlreadyInactiveException("Paciente já está inativado");
        }

        List<EQueueStatus> pendingStatuses = List.of(EQueueStatus.AGUARDANDO, EQueueStatus.AGENDADO);
        List<QueueEntry> pendingQueueEntries = queueEntryRepository.findByPatientAndStatusIn(patient, pendingStatuses);

        if (!pendingQueueEntries.isEmpty()) {
            throw new PatientHasActiveQueueEntriesException(
                    "Paciente possui entradas ativas na fila e não pode ser inativado");
        }

        patient.inactivate();
        patientRepository.save(patient);

        return patient;
    }
}
