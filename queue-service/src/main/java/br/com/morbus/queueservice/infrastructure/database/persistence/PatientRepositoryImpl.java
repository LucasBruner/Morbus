package br.com.morbus.queueservice.infrastructure.database.persistence;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.repository.IPatientRepository;
import br.com.morbus.queueservice.infrastructure.database.entity.PatientEntity;
import br.com.morbus.queueservice.infrastructure.database.repository.PatientJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PatientRepositoryImpl implements IPatientRepository {

    private final PatientJpaRepository repository;

    public PatientRepositoryImpl(PatientJpaRepository repository) {
        this.repository = repository;
    }

    public void save(Patient patient) {
        if (patient.getId() != null) {
            repository.findById(patient.getId()).ifPresentOrElse(
                    existing -> {
                        updateEntityFields(existing, patient);
                        repository.save(existing);
                    },
                    () -> repository.save(mapToEntityPatient(patient))
            );
        } else {
            repository.save(mapToEntityPatient(patient));
        }
    }

    @Override
    public Optional<Patient> findById(UUID id) {
        Optional<PatientEntity> patient = repository.findById(id);
        return patient.map(this::mapToDomainPatient);
    }

    @Override
    public Optional<Patient> findByCpf(String cpf) {
        Optional<PatientEntity> patient = repository.findByCpf(cpf);
        return patient.map(this::mapToDomainPatient);
    }

    @Override
    public Optional<Patient> findByCns(String cns) {
        Optional<PatientEntity> patient = repository.findByCns(cns);
        return patient.map(this::mapToDomainPatient);
    }

    @Override
    public List<Patient> findAll() {
        List<PatientEntity> patientList = repository.findAll();

        return patientList.stream()
                .map(this::mapToDomainPatient)
                .toList();
    }

    Patient mapToDomainPatient(PatientEntity entity) {
        return Patient.builder()
                .id(entity.getId())
                .cpf(entity.getCpf())
                .cns(entity.getCns())
                .nome(entity.getNome())
                .sobrenome(entity.getSobrenome())
                .dataNascimento(entity.getDataNascimento())
                .gender(entity.getSexo())
                .contato(entity.getContato())
                .grupoLegal(entity.getGrupoLegal())
                .ativo(entity.getAtivo() != null && entity.getAtivo())
                .build();
    }

    PatientEntity mapToEntityPatient(Patient patient) {
        return PatientEntity.builder()
                .id(patient.getId())
                .cpf(patient.getCpf())
                .cns(patient.getCns())
                .nome(patient.getNome())
                .sobrenome(patient.getSobrenome())
                .dataNascimento(patient.getDataNascimento())
                .sexo(patient.getGender())
                .contato(patient.getContato())
                .grupoLegal(patient.getGrupoLegal())
                .build();
    }
    
    private void updateEntityFields(PatientEntity existing, Patient patient) {
        existing.setCpf(patient.getCpf());
        existing.setCns(patient.getCns());
        existing.setNome(patient.getNome());
        existing.setSobrenome(patient.getSobrenome());
        existing.setDataNascimento(patient.getDataNascimento());
        existing.setSexo(patient.getGender());
        existing.setContato(patient.getContato());
        existing.setGrupoLegal(patient.getGrupoLegal());
        existing.setAtivo(patient.isAtivo());
    }
}
