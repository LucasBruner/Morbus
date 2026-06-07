package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.exception.PatientNotFoundException;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
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
        Patient patient = patientRepository.findByCpf(dto.cpf())
                .orElseThrow(() -> new PatientNotFoundException("Paciente não cadastrado"));

        patient.update(dto);
        patientRepository.save(patient);

        return patient;
    }
}
