package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.exception.PatientNotFoundException;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;

public class GetPatientByCpf {

    private final IPatientRepository patientRepository;

    private GetPatientByCpf(IPatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public static GetPatientByCpf create(IPatientRepository patientRepository) {
        return new GetPatientByCpf(patientRepository);
    }

    public Patient run(String cpf) {
        return patientRepository.findByCpf(cpf)
                .orElseThrow(() -> new PatientNotFoundException(
                        "Paciente com CPF " + cpf + " não encontrado"));
    }
}
