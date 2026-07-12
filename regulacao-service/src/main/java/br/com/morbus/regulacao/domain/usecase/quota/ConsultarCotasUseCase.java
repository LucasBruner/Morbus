package br.com.morbus.regulacao.domain.usecase.quota;

import br.com.morbus.regulacao.domain.dto.PageResult;
import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.ports.in.IConsultarCotasUseCase;
import br.com.morbus.regulacao.ports.in.dto.ConsultarCotasQuery;
import br.com.morbus.regulacao.ports.out.IQuotaRepository;

public class ConsultarCotasUseCase implements IConsultarCotasUseCase {

    private final IQuotaRepository quotaRepository;

    public ConsultarCotasUseCase(IQuotaRepository quotaRepository) {
        this.quotaRepository = quotaRepository;
    }

    @Override
    public PageResult<Quota> execute(ConsultarCotasQuery query) {
        return quotaRepository.listar(query);
    }
}
