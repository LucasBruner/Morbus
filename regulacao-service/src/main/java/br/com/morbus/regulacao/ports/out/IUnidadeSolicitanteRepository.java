package br.com.morbus.regulacao.ports.out;

import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;

import java.util.Optional;
import java.util.UUID;

public interface IUnidadeSolicitanteRepository {
    UnidadeSolicitante salvar(UnidadeSolicitante unidade);
    Optional<UnidadeSolicitante> buscarPorId(UUID id);
    boolean existsById(UUID id);
}