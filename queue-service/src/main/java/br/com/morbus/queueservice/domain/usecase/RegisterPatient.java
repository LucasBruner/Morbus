package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.exception.PatientAlreadyExistsException;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.service.PriorityCalculator;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterPatientDTO;

import java.util.UUID;

public class RegisterPatient {

    private final IPatientRepository patientRepository;

    private RegisterPatient(IPatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public static RegisterPatient create(IPatientRepository patientRepository) {
        return new RegisterPatient(patientRepository);
    }

    public Patient run(RegisterPatientDTO dto) {
        if (patientRepository.findByCpf(dto.cpf()).isPresent()) {
            throw new PatientAlreadyExistsException("Paciente com CPF " + dto.cpf() + " já existe");
        }

        if (dto.cns() != null && !dto.cns().isBlank() && patientRepository.findByCns(dto.cns()).isPresent()) {
            throw new PatientAlreadyExistsException("Paciente com CNS " + dto.cns() + " já existe");
        }

        Patient patient = Patient.builder()
                .id(UUID.randomUUID())
                .cpf(dto.cpf())
                .cns(dto.cns())
                .nome(dto.nome())
                .sobrenome(dto.sobrenome())
                .dataNascimento(dto.dataNascimento())
                .gender(dto.gender())
                .contato(dto.contato())
                .ativo(true)
                .grupoLegal(dto.grupoLegal())
                .build();

        EPriorityGroup priorityGroup = PriorityCalculator.getPriorityGroup(patient);
        patient.updateGrupoLegal(priorityGroup);

        patientRepository.save(patient);

        return patient;
    }
}
