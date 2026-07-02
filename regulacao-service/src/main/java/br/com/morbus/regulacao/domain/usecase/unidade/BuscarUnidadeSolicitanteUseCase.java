package br.com.morbus.regulacao.domain.usecase.unidade;

import br.com.morbus.regulacao.domain.exception.UnidadeSolicitanteNaoEncontradaException;
import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;
import br.com.morbus.regulacao.ports.in.IBuscarUnidadeSolicitanteUseCase;
import br.com.morbus.regulacao.ports.out.IUnidadeSolicitanteRepository;

import java.util.UUID;

public class BuscarUnidadeSolicitanteUseCase implements IBuscarUnidadeSolicitanteUseCase {

    private final IUnidadeSolicitanteRepository unidadeSolicitanteRepository;

    public BuscarUnidadeSolicitanteUseCase(IUnidadeSolicitanteRepository unidadeSolicitanteRepository) {
        this.unidadeSolicitanteRepository = unidadeSolicitanteRepository;
    }

    @Override
    public UnidadeSolicitante execute(UUID id) {
        return unidadeSolicitanteRepository.buscarPorId(id)
                .orElseThrow(() -> new UnidadeSolicitanteNaoEncontradaException(
                        "Id informado nao possui nenhuma unidade solicitante cadastrada: %s".formatted(id)));
    }
}