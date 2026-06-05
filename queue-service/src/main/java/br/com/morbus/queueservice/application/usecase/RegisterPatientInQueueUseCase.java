package br.com.morbus.queueservice.application.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.event.PatientRegisteredEvent;
import br.com.morbus.queueservice.domain.exception.PatientNotEligibleForProcedureException;
import br.com.morbus.queueservice.domain.exception.ProcedureNotFoundException;
import br.com.morbus.queueservice.domain.port.PatientRegisteredEventPublisher;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.service.PriorityCalculator;
import br.com.morbus.queueservice.domain.usecase.RegisterPatientCommand;
import br.com.morbus.queueservice.domain.usecase.RegisterPatientInQueue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;
import java.util.UUID;

public class RegisterPatientInQueueUseCase implements RegisterPatientInQueue {

    private final IPatientRepository patientRepository;
    private final IProcedureRepository procedureRepository;
    private final IQueueEntryRepository queueEntryRepository;
    private final PatientRegisteredEventPublisher eventPublisher;

    public RegisterPatientInQueueUseCase(IPatientRepository patientRepository,
                                         IProcedureRepository procedureRepository,
                                         IQueueEntryRepository queueEntryRepository,
                                         PatientRegisteredEventPublisher eventPublisher) {
        this.patientRepository = patientRepository;
        this.procedureRepository = procedureRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public QueueEntry execute(RegisterPatientCommand command) {
        validateCommand(command);

        Procedure procedure = procedureRepository.findById(command.getProcedureId())
                .orElseThrow(() -> new ProcedureNotFoundException(command.getProcedureId()));

        Optional<Patient> existingPatient = findExistingPatient(command.getPatient());
        Patient patient = existingPatient.orElse(command.getPatient());
        validatePatientAge(patient.getDataNascimento(), procedure);

        EPriorityGroup priorityGroup = PriorityCalculator.getPriorityGroup(patient);
        patient.setGrupoLegal(priorityGroup);

        if (existingPatient.isEmpty()) {
            patientRepository.save(patient);
        }

        QueueEntry queueEntry = buildQueueEntry(patient, procedure);
        queueEntryRepository.save(queueEntry);

        eventPublisher.publish(new PatientRegisteredEvent(patient, queueEntry));
        return queueEntry;
    }

    private void validateCommand(RegisterPatientCommand command) {
        if (command == null || command.getPatient() == null || command.getProcedureId() == null) {
            throw new IllegalArgumentException("Comando de registro inválido");
        }
    }

    private Optional<Patient> findExistingPatient(Patient candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("Paciente não pode ser nulo");
        }

        Optional<Patient> foundByCpf = candidate.getCpf() != null
                ? patientRepository.findByCpf(candidate.getCpf())
                : Optional.empty();
        Optional<Patient> foundByCns = candidate.getCns() != null
                ? patientRepository.findByCns(candidate.getCns())
                : Optional.empty();

        return foundByCpf.or(() -> foundByCns);
    }

    private void validatePatientAge(LocalDate birthDate, Procedure procedure) {
        if (birthDate == null) {
            throw new PatientNotEligibleForProcedureException("Data de nascimento do paciente é obrigatória");
        }

        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < procedure.getIdadeMinima() || age > procedure.getIdadeMaxima()) {
            throw new PatientNotEligibleForProcedureException(
                    String.format("Paciente com %d anos não é elegível para procedimento %s", age, procedure.getNoProcedimento())
            );
        }
    }

    private QueueEntry buildQueueEntry(Patient patient, Procedure procedure) {
        QueueEntry queueEntry = new QueueEntry();
        queueEntry.setId(UUID.randomUUID());
        queueEntry.setPatient(patient);
        queueEntry.setProcedure(procedure);
        queueEntry.setRiskColor(ERiskColor.AZUL);
        queueEntry.setQueueStatus(EQueueStatus.AGUARDANDO);
        queueEntry.setRegisteredAt(LocalDateTime.now());
        queueEntry.setUpdatedAt(queueEntry.getRegisteredAt());
        queueEntry.setPosicaoCalculada(0);
        return queueEntry;
    }
}
