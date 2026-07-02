package br.com.morbus.regulacao.ports.in;

import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.ports.in.dto.GerenciarCotaCommand;

public interface IGerenciarCotaUseCase {
    Quota execute(GerenciarCotaCommand command);
}
