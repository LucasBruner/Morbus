package br.com.morbus.queueservice.domain.repository;

import br.com.morbus.queueservice.domain.entity.Procedure;

import java.util.List;
import java.util.Optional;

public interface IProcedureRepository {
    void save();
    Optional<Procedure> findById(Integer id);
    Optional<Procedure> findByCoProcedimento(String coProcedimento);
    List<Procedure> findAll();
}
