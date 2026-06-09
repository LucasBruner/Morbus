package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.exception.PatientAgeNotEligibleException;
import br.com.morbus.queueservice.domain.exception.PatientInactiveException;
import br.com.morbus.queueservice.domain.exception.PatientNotFoundException;
import br.com.morbus.queueservice.domain.exception.ProcedureAlreadyAssignedException;
import br.com.morbus.queueservice.domain.exception.ProcedureNotFoundException;
import br.com.morbus.queueservice.domain.repository.IPatientProcedureRepository;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;
import br.com.morbus.queueservice.domain.usecase.dto.IdProcedureAndPatientDTO;

public class AssignProcedureToPatient {
    private final IPatientProcedureRepository patientProcedureRepository;
    private final IPatientRepository patientRepository;
    private final IProcedureRepository procedureRepository;

    private AssignProcedureToPatient(IPatientProcedureRepository patientProcedureRepository,
                                    IPatientRepository patientRepository,
                                    IProcedureRepository procedureRepository) {
        this.patientProcedureRepository = patientProcedureRepository;
        this.patientRepository = patientRepository;
        this.procedureRepository = procedureRepository;
    }

    public static AssignProcedureToPatient create(IPatientProcedureRepository patientProcedureRepository,
                                                  IPatientRepository patientRepository,
                                                  IProcedureRepository procedureRepository) {
        return new AssignProcedureToPatient(patientProcedureRepository,
                patientRepository,
                procedureRepository);
    }

    public Procedure run(IdProcedureAndPatientDTO idProcedureAndPatientDTO) {
        Patient patient = patientRepository
                .findById(idProcedureAndPatientDTO
                        .patientId())
                .orElseThrow(()
                        -> new PatientNotFoundException("Paciente não encontrado"));

        if(!patient.isAtivo()) {
            throw new PatientInactiveException("Paciente não está ativo");
        }

        Procedure procedure = procedureRepository
                .findById(idProcedureAndPatientDTO
                        .procedureId())
                .orElseThrow(()
                        -> new ProcedureNotFoundException(idProcedureAndPatientDTO.procedureId()));

        if(!procedure.isAgeEligible(patient.getDataNascimento())) {
            throw new PatientAgeNotEligibleException("Paciente não está dentro da idade permitida");
        }
        if (patientProcedureRepository.existsByPatientAndProcedure(patient.getId(), procedure.getId())) {
           throw new ProcedureAlreadyAssignedException("Procedimento já atribuído ao paciente");
        }
        patientProcedureRepository.save(patient.getId(), procedure.getId());

        return procedure;
    }
}
