package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.exception.ProcedureNotFoundException;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;

import java.util.UUID;

public class GetProcedureById {

    private final IProcedureRepository procedureRepository;

    private GetProcedureById(IProcedureRepository procedureRepository) {
        this.procedureRepository = procedureRepository;
    }

    public static GetProcedureById create(IProcedureRepository procedureRepository) {
        return new GetProcedureById(procedureRepository);
    }

    public Procedure run(UUID id) {
        return procedureRepository.findById(id)
                .orElseThrow(() -> new ProcedureNotFoundException(id));
    }
}
