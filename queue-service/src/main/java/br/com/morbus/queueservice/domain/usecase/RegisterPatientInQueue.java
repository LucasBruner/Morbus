package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.domain.exception.*;
import br.com.morbus.queueservice.domain.usecase.DTO.RegisterPatientInQueueDTO;
import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.service.PriorityCalculator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

public class RegisterPatientInQueue {

    private final IPatientRepository patientRepository;
    private final IProcedureRepository procedureRepository;
    private final IQueueEntryRepository queueEntryRepository;
    private final IQueueEventPublisher eventPublisher;

    private RegisterPatientInQueue(IPatientRepository patientRepository, IProcedureRepository procedureRepository, IQueueEntryRepository queueEntryRepository, IQueueEventPublisher eventPublisher) {
        this.patientRepository = patientRepository;
        this.procedureRepository = procedureRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.eventPublisher = eventPublisher;
    }

    public static RegisterPatientInQueue create(IPatientRepository patientRepository, IProcedureRepository procedureRepository, IQueueEntryRepository queueEntryRepository, IQueueEventPublisher eventPublisher) {
        return new RegisterPatientInQueue(patientRepository, procedureRepository, queueEntryRepository, eventPublisher);
    }

    public QueueEntry execute(RegisterPatientInQueueDTO patientDTO) {
        validatePatient(patientDTO);

        Patient patient = patientRepository.findById(patientDTO.patient().getId())
                .orElseThrow(() -> new PatientNotFoundException("Paciente não encontrado"));

        Procedure procedure = procedureRepository.findById(patientDTO.procedureId())
                .orElseThrow(() -> new ProcedureNotFoundException(patientDTO.procedureId()));

        validateIfActivePatient(patient);
        validateIfPatientInQueue(patient, procedure);
        validatePatientAge(patient.getDataNascimento(), procedure);

        EPriorityGroup priorityGroup = PriorityCalculator.getPriorityGroup(patient);
        patient.updateGrupoLegal(priorityGroup);
        patientRepository.save(patient);

        ERiskColor riskColor = patientDTO.riskColor() == null ? ERiskColor.AZUL : patientDTO.riskColor();
        QueueEntry queueEntry = buildQueueEntry(patient, procedure, riskColor);

        queueEntryRepository.save(queueEntry);
        eventPublisher.publishPatientRegistered(queueEntry);

        return queueEntry;
    }

    private void validatePatient(RegisterPatientInQueueDTO patientDTO) {
        if (patientDTO == null || patientDTO.patient() == null || patientDTO.procedureId() == null) {
            throw new IllegalArgumentException("Comando de registro inválido");
        }
    }

    private void validateIfActivePatient(Patient patient) {
        if (!patient.isAtivo()) {
            throw new PatientInactiveException("O paciente está inativo");
        }
    }

    private void validatePatientAge(LocalDate birthDate, Procedure procedure) {
        if (birthDate == null) {
            throw new PatientNotEligibleForProcedureException("Data de nascimento do paciente é obrigatória");
        }

        LocalDate today = LocalDate.now();
        int age = Period.between(birthDate, today).getYears();
        if (age < procedure.getIdadeMinima() || age > procedure.getIdadeMaxima()) {
            throw new PatientNotEligibleForProcedureException(
                    String.format("Paciente com %d anos não é elegível para procedimento %s", age, procedure.getNoProcedimento())
            );
        }
    }

    private void validateIfPatientInQueue(Patient patient, Procedure procedure) {
        boolean existsActiveEntry = queueEntryRepository.existsByPatientAndProcedureAndStatusIn(
                patient,
                procedure,
                java.util.List.of(EQueueStatus.AGUARDANDO, EQueueStatus.AGENDADO)
        );

        if (existsActiveEntry) {
            throw new PatientAlreadyRegisteredException(
                    "Paciente já está registrado na fila (AGUARDANDO ou AGENDADO) para este procedimento"
            );
        }
    }

    private QueueEntry buildQueueEntry(Patient patient, Procedure procedure, ERiskColor riskColor) {
        return QueueEntry.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .procedure(procedure)
                .riskColor(riskColor)
                .queueStatus(EQueueStatus.AGUARDANDO)
                .registeredAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
