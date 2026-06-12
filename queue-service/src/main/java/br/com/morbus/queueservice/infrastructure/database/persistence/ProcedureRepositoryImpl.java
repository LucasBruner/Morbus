package br.com.morbus.queueservice.infrastructure.database.persistence;

import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ProcedureRepositoryImpl implements IProcedureRepository {
    @Override
    public void save(Procedure procedure) {

    }

    @Override
    public Optional<Procedure> findById(UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<Procedure> findByCoProcedimento(String coProcedimento) {
        return Optional.empty();
    }

    @Override
    public List<Procedure> findAll() {
        return List.of();
    }
}
