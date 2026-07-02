package br.com.morbus.regulacao.ports.in;

import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;

import java.util.UUID;

public interface IBuscarUnidadeSolicitanteUseCase {
    UnidadeSolicitante execute(UUID id);
}