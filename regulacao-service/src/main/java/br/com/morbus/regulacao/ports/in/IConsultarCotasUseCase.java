package br.com.morbus.regulacao.ports.in;

import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.ports.in.dto.ConsultarCotasQuery;
import org.springframework.data.domain.Page;

public interface IConsultarCotasUseCase {
    Page<Quota> execute(ConsultarCotasQuery query);
}
