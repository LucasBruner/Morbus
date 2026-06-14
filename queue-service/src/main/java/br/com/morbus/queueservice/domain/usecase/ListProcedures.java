package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.repository.IProcedureRepository;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public class ListProcedures {

    private final IProcedureRepository procedureRepository;

    private ListProcedures(IProcedureRepository procedureRepository) {
        this.procedureRepository = procedureRepository;
    }

    public static ListProcedures create(IProcedureRepository procedureRepository) {
        return new ListProcedures(procedureRepository);
    }

    public List<Procedure> run(int page, int size) {
        // page é 1-based na API; PageRequest é 0-based
        return procedureRepository.findAll(PageRequest.of(page - 1, size));
    }
}
