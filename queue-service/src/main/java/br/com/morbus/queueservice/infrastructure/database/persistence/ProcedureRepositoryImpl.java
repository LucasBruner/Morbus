package br.com.morbus.queueservice.infrastructure.database.persistence;

import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;
import br.com.morbus.queueservice.infrastructure.database.entity.ProcedureEntity;
import br.com.morbus.queueservice.infrastructure.database.repository.ProcedureJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProcedureRepositoryImpl implements IProcedureRepository {

    private final ProcedureJpaRepository repository;

    public ProcedureRepositoryImpl(ProcedureJpaRepository procedureJpaRepository) {
        this.repository = procedureJpaRepository;
    }

    @Override
    public void save(Procedure procedure) {
        ProcedureEntity procedureEntity = mapToEntityProcedure(procedure);
        repository.save(procedureEntity);
    }

    @Override
    public Optional<Procedure> findById(UUID id) {
        Optional<ProcedureEntity> procedure = repository.findById(id);
        return procedure.map(this::mapToDomainProcedure);
    }

    @Override
    public Optional<Procedure> findByCoProcedimento(String coProcedimento) {
        Optional<ProcedureEntity> procedure = repository.findByCoProcedimento(coProcedimento);
        return procedure.map(this::mapToDomainProcedure);
    }

    @Override
    public List<Procedure> findAll(PageRequest pageRequest) {
        List<ProcedureEntity> procedureList = repository.findAll();

        return procedureList.stream()
                .map(this::mapToDomainProcedure)
                .toList();
    }

    Procedure mapToDomainProcedure(ProcedureEntity procedure) {
        return Procedure.builder()
                .id(procedure.getId())
                .coProcedimento(procedure.getCoProcedimento())
                .noProcedimento(procedure.getNoProcedimento())
                .idadeMinima(procedure.getIdadeMinima())
                .idadeMaxima(procedure.getIdadeMaxima())
                .grupo(procedure.getGrupo())
                .build();
    }

    ProcedureEntity mapToEntityProcedure(Procedure procedure) {
        return ProcedureEntity.builder()
                .id(procedure.getId())
                .coProcedimento(procedure.getCoProcedimento())
                .noProcedimento(procedure.getNoProcedimento())
                .idadeMinima(procedure.getIdadeMinima())
                .idadeMaxima(procedure.getIdadeMaxima())
                .grupo(procedure.getGrupo())
                .build();
    }
}
