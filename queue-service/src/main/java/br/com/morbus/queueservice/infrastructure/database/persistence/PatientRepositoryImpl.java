package br.com.morbus.queueservice.infrastructure.database.persistence;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterPatientDTO;
import br.com.morbus.queueservice.infrastructure.database.entity.PatientEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PatientRepositoryImpl implements IPatientRepository {
    @Override
    public void save(Patient patient) {

    }

    @Override
    public Optional<Patient> findById(UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<Patient> findByCpf(String cpf) {
        return Optional.empty();
    }

    @Override
    public Optional<Patient> findByCns(String cns) {
        return Optional.empty();
    }

    @Override
    public List<Patient> findAll() {
        return List.of();
    }

    private Patient toDomain(Patient entity) {
        return Patient.builder()
                .id(entity.getId())
                .cpf(entity.getCpf())
                .cns(entity.getCns())
                .nome(entity.getNome())
                .sobrenome(entity.getSobrenome())
                .dataNascimento(entity.getDataNascimento())
                .gender(entity.getGender())
                .contato(entity.getContato())
                .grupoLegal(entity.getGrupoLegal())
                .build();
    }

    private PatientEntity toEntity(RegisterPatientDTO patient) {
        return PatientEntity.builder()
                .cpf(patient.cpf())
                .cns(patient.cns())
                .nome(patient.nome())
                .sobrenome(patient.sobrenome())
                .dataNascimento(patient.dataNascimento())
                .sexo(patient.gender())
                .contato(patient.contato())
                .grupoLegal(patient.grupoLegal())
                .build();
    }
}
