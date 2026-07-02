package br.com.morbus.regulacao.domain.usecase.unidade;

import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;
import br.com.morbus.regulacao.ports.in.ICadastrarUnidadeSolicitanteUseCase;
import br.com.morbus.regulacao.ports.in.dto.CadastrarUnidadeSolicitanteCommand;
import br.com.morbus.regulacao.ports.out.IUnidadeSolicitanteRepository;

public class CadastrarUnidadeSolicitanteUseCase implements ICadastrarUnidadeSolicitanteUseCase {

    private final IUnidadeSolicitanteRepository unidadeSolicitanteRepository;

    public CadastrarUnidadeSolicitanteUseCase(IUnidadeSolicitanteRepository unidadeSolicitanteRepository) {
        this.unidadeSolicitanteRepository = unidadeSolicitanteRepository;
    }

    @Override
    public UnidadeSolicitante execute(CadastrarUnidadeSolicitanteCommand command) {
        UnidadeSolicitante unidade = new UnidadeSolicitante(
                command.cnes(), command.nome(), command.endereco(), command.telefone());
        return unidadeSolicitanteRepository.salvar(unidade);
    }
}