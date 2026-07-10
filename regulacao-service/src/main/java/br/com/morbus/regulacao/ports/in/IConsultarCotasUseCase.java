package br.com.morbus.regulacao.ports.in;

import br.com.morbus.regulacao.domain.dto.PageResult;
import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.ports.in.dto.ConsultarCotasQuery;

public interface IConsultarCotasUseCase {
    PageResult<Quota> execute(ConsultarCotasQuery query);
}
