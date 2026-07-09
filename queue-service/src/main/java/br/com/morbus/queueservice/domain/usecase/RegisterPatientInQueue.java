package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.event.IQueueEventPublisher;
import br.com.morbus.queueservice.domain.exception.*;
import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EDestino;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.service.PriorityCalculator;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterQueueRequestDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public QueueEntry execute(RegisterQueueRequestDTO patientDTO) {
        validatePatient(patientDTO);

        Patient patient = patientRepository.findById(patientDTO.patientId())
                .orElseThrow(() -> new PatientNotFoundException("Paciente não encontrado"));

        Procedure procedure = procedureRepository.findById(patientDTO.procedureId())
                .orElseThrow(() -> new ProcedureNotFoundException(patientDTO.procedureId()));

        validateIfActivePatient(patient);
        validateIfPatientInQueue(patient, procedure);
        validatePatientAge(patient.getDataNascimento(), procedure);

        EPriorityGroup priorityGroup = PriorityCalculator.getPriorityGroup(patient);
        patient.updateGrupoLegal(priorityGroup);
        patientRepository.save(patient);

        EDestino tipoFila = patientDTO.tipoFila() == null ? EDestino.FILA_REGULADA : patientDTO.tipoFila();
        ERiskColor riskColor = patientDTO.riskColor() == null ? ERiskColor.AZUL : patientDTO.riskColor();
        validateTipoFilaRiskColor(tipoFila, riskColor);

        QueueEntry queueEntry = buildQueueEntry(patient, procedure, riskColor, tipoFila);

        queueEntryRepository.save(queueEntry);
        eventPublisher.publishPatientRegistered(queueEntry);

        return queueEntry;
    }

    private void validatePatient(RegisterQueueRequestDTO patientDTO) {
        if (patientDTO == null || patientDTO.patientId() == null || patientDTO.procedureId() == null) {
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

        if (!procedure.isAgeEligible(birthDate)) {
            throw new PatientNotEligibleForProcedureException(
                    String.format("Paciente com idade não compatível para o procedimento %s", procedure.getNoProcedimento())
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

    private void validateTipoFilaRiskColor(EDestino tipoFila, ERiskColor riskColor) {
        if (tipoFila == EDestino.FILA_ESPERA && riskColor != ERiskColor.AZUL) {
            throw new QueueNotAllowedException("FILA_ESPERA aceita somente pacientes com cor de risco AZUL");
        }
    }

    private QueueEntry buildQueueEntry(Patient patient, Procedure procedure, ERiskColor riskColor, EDestino tipoFila) {
        return QueueEntry.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .procedure(procedure)
                .riskColor(riskColor)
                .tipoFila(tipoFila)
                .queueStatus(EQueueStatus.AGUARDANDO)
                .registeredAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
