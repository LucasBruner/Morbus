package br.com.morbus.queueservice.domain.repository;

import br.com.morbus.queueservice.domain.entity.Procedure;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.UUID;

public interface IProcedureRepository {
    void save(Procedure procedure);
    Optional<Procedure> findById(UUID id);
    Optional<Procedure> findByCoProcedimento(String coProcedimento);
    ProcedurePageResult findAll(PageRequest pageRequest);
}
