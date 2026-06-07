package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.exception.PatientNotFoundException;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.service.PriorityCalculator;
import br.com.morbus.queueservice.domain.usecase.DTO.UpdatePatientDTO;

public class UpdatePatient {

    private final IPatientRepository patientRepository;

    private UpdatePatient(IPatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public static UpdatePatient create(IPatientRepository patientRepository) {
        return new UpdatePatient(patientRepository);
    }

    public Patient run(UpdatePatientDTO dto) {
        Patient patient = patientRepository.findById(dto.id())
                .orElseThrow(() -> new PatientNotFoundException("Paciente não cadastrado"));

        patient.update(dto);

        EPriorityGroup priorityGroup = PriorityCalculator.getPriorityGroup(patient);
        patient.updateGrupoLegal(priorityGroup);

        patientRepository.save(patient);

        return patient;
    }
}
