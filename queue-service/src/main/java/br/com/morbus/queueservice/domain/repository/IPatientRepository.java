package br.com.morbus.queueservice.domain.repository;

import br.com.morbus.queueservice.domain.entity.Patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPatientRepository {
    void save(Patient patient);
    Optional<Patient> findById(UUID id);
    Optional<Patient> findByCpf(String cpf);
    Optional<Patient> findByCns(String cns);
    List<Patient> findAll();
}
