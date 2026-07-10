package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.repository.IProcedureRepository;
import br.com.morbus.queueservice.domain.repository.ProcedurePageResult;
import org.springframework.data.domain.PageRequest;

public class ListProcedures {

    public static final int MAX_SIZE = 100;

    private final IProcedureRepository procedureRepository;

    private ListProcedures(IProcedureRepository procedureRepository) {
        this.procedureRepository = procedureRepository;
    }

    public static ListProcedures create(IProcedureRepository procedureRepository) {
        return new ListProcedures(procedureRepository);
    }

    public ProcedurePageResult run(int page, int size) {
        // page é 0-based, conforme o contrato de API
        int effectiveSize = Math.min(size, MAX_SIZE);
        return procedureRepository.findAll(PageRequest.of(page, effectiveSize));
    }
}
