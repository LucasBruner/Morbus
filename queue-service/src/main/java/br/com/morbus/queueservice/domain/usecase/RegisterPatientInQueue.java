package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.usecase.DTO.RegisterPatientDTO;
import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.event.IPatientRegisterEventPublisher;
import br.com.morbus.queueservice.domain.exception.PatientNotEligibleForProcedureException;
import br.com.morbus.queueservice.domain.exception.PatientNotFoundException;
import br.com.morbus.queueservice.domain.exception.ProcedureNotFoundException;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.service.PriorityCalculator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;
import java.util.UUID;

public class RegisterPatientInQueue {

    private final IPatientRepository patientRepository;
    private final IProcedureRepository procedureRepository;
    private final IQueueEntryRepository queueEntryRepository;
    private final IPatientRegisterEventPublisher eventPublisher;

    private RegisterPatientInQueue(IPatientRepository patientRepository, IProcedureRepository procedureRepository, IQueueEntryRepository queueEntryRepository, IPatientRegisterEventPublisher eventPublisher) {
        this.patientRepository = patientRepository;
        this.procedureRepository = procedureRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.eventPublisher = eventPublisher;
    }

    public Patient execute(RegisterPatientDTO patientDTO) {
        validatePatient(patientDTO);

        Procedure procedure = procedureRepository.findById(patientDTO.procedureId())
                .orElseThrow(() -> new ProcedureNotFoundException(patientDTO.procedureId()));

        Patient patient = patientRepository.findById(patientDTO.patient().getId())
                .orElseThrow(() -> new PatientNotFoundException("Paciente não encontrado"));

        validatePatientAge(patient.getDataNascimento(), procedure);

        EPriorityGroup priorityGroup = PriorityCalculator.getPriorityGroup(patient);
        patient.updateGrupoLegal(priorityGroup);
        patientRepository.save(patient);

        Optional<QueueEntry> queueEntry = queueEntryRepository.findByPatient(patient);
        //buildQueueEntry(patientDTO);
        //queueEntryRepository.save(queueEntry);

        eventPublisher.publish(patient);
        return patient;
    }

    private void validatePatient(RegisterPatientDTO patientDTO) {
        if (patientDTO == null || patientDTO.patient() == null || patientDTO.procedureId() == null) {
            throw new IllegalArgumentException("Comando de registro inválido");
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

    private QueueEntry buildQueueEntry(RegisterPatientDTO patientDTO) {
        return QueueEntry.builder()
                .id(UUID.randomUUID())
                .patient(patientDTO.patient())
                .riskColor(ERiskColor.AZUL)
                .queueStatus(EQueueStatus.AGUARDANDO)
                .registeredAt(LocalDateTime.now())
                .build();
    }
}
