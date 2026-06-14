package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.exception.ProcedureNotFoundException;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;

public class GetProcedureByCodigo {

    private final IProcedureRepository procedureRepository;

    private GetProcedureByCodigo(IProcedureRepository procedureRepository) {
        this.procedureRepository = procedureRepository;
    }

    public static GetProcedureByCodigo create(IProcedureRepository procedureRepository) {
        return new GetProcedureByCodigo(procedureRepository);
    }

    public Procedure run(String codigo) {
        return procedureRepository.findByCoProcedimento(codigo)
                .orElseThrow(() -> new ProcedureNotFoundException(
                        "Procedimento com código SIGTAP " + codigo + " não encontrado"));
    }
}
