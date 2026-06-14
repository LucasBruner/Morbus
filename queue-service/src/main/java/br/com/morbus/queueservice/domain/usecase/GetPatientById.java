package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.exception.PatientNotFoundException;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;

import java.util.UUID;

public class GetPatientById {

    private final IPatientRepository patientRepository;

    private GetPatientById(IPatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public static GetPatientById create(IPatientRepository patientRepository) {
        return new GetPatientById(patientRepository);
    }

    public Patient run(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(
                        "Paciente com ID " + id + " não encontrado"));
    }
}
