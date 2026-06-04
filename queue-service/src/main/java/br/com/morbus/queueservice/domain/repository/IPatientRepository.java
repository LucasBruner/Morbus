package br.com.morbus.queueservice.domain.repository;

import br.com.morbus.queueservice.domain.entity.Patient;

import java.util.List;
import java.util.Optional;

public interface IPatientRepository {
    void save();
    Optional<Patient> findById(Integer id);
    Optional<Patient> findByCpf(String cpf);
    Optional<Patient> findByCns(String cns);
    List<Patient> findAll();
}
