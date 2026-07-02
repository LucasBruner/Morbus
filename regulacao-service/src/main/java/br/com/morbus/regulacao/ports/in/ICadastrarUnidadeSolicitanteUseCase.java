package br.com.morbus.regulacao.ports.in;

import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;
import br.com.morbus.regulacao.ports.in.dto.CadastrarUnidadeSolicitanteCommand;

public interface ICadastrarUnidadeSolicitanteUseCase {
    UnidadeSolicitante execute(CadastrarUnidadeSolicitanteCommand command);
}