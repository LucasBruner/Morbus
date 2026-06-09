package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.exception.ActiveQueueEntriesExistException;
import br.com.morbus.queueservice.domain.exception.PatientNotFoundException;
import br.com.morbus.queueservice.domain.exception.ProcedureNotAssignedException;
import br.com.morbus.queueservice.domain.exception.ProcedureNotFoundException;
import br.com.morbus.queueservice.domain.repository.IPatientProcedureRepository;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;
import br.com.morbus.queueservice.domain.repository.IQueueEntryRepository;
import br.com.morbus.queueservice.domain.usecase.dto.IdProcedureAndPatientDTO;

import java.util.List;

public class RemoveProcedureFromPatient {
    private final IPatientProcedureRepository patientProcedureRepository;
    private final IQueueEntryRepository queueEntryRepository;
    private final IPatientRepository patientRepository;
    private final IProcedureRepository procedureRepository;

    private RemoveProcedureFromPatient(IPatientProcedureRepository patientProcedureRepository,
                                       IQueueEntryRepository queueEntryRepository,
                                       IPatientRepository patientRepository,
                                       IProcedureRepository procedureRepository) {
        this.patientProcedureRepository = patientProcedureRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.patientRepository = patientRepository;
        this.procedureRepository = procedureRepository;
    }

    public static RemoveProcedureFromPatient create(IPatientProcedureRepository patientProcedureRepository,
                                                    IQueueEntryRepository queueEntryRepository,
                                                    IPatientRepository patientRepository,
                                                    IProcedureRepository procedureRepository) {
        return new RemoveProcedureFromPatient(patientProcedureRepository,
                queueEntryRepository,
                patientRepository,
                procedureRepository);
    }

    public void run(IdProcedureAndPatientDTO idProcedureAndPatientDTO) {
        Patient patient = patientRepository
                .findById(idProcedureAndPatientDTO
                        .patientId()).orElseThrow(
                                () -> new PatientNotFoundException("Paciente não encontrado"));

        if(!patientProcedureRepository.existsByPatientAndProcedure(patient.getId(), idProcedureAndPatientDTO.procedureId())) {
            throw new ProcedureNotAssignedException("Procedimento não atribuído ao paciente");
        }

        Procedure procedure = procedureRepository
                .findById(idProcedureAndPatientDTO
                        .procedureId())
                .orElseThrow(
                        () -> new ProcedureNotFoundException(idProcedureAndPatientDTO.procedureId()));
        List<EQueueStatus> queueStatuses = List.of(EQueueStatus.AGUARDANDO, EQueueStatus.AGENDADO);

        if(queueEntryRepository.existsByPatientAndProcedureAndStatusIn(patient, procedure, queueStatuses)) {
            throw new ActiveQueueEntriesExistException("Já existe um procedimento agendado ou aguardando para este paciente. Faça a remoção manual da fila!");
        }

        patientProcedureRepository.deleteByPatientAndProcedure(patient.getId(), procedure.getId());
        patientProcedureRepository.save(patient.getId(), procedure.getId());
    }
}
