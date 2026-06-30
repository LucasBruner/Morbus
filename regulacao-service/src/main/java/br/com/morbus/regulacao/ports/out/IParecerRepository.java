package br.com.morbus.regulacao.ports.out;

import br.com.morbus.regulacao.domain.model.Parecer;

public interface IParecerRepository {
    Parecer save(Parecer parecer);
}
