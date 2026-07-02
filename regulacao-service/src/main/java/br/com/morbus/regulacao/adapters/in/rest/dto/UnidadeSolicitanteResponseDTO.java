package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;

import java.util.UUID;

public record UnidadeSolicitanteResponseDTO(UUID id, String cnes, String nome, String endereco, String telefone) {

    public static UnidadeSolicitanteResponseDTO fromDomain(UnidadeSolicitante unidade) {
        return new UnidadeSolicitanteResponseDTO(unidade.getId(), unidade.getCnes(), unidade.getNome(),
                unidade.getEndereco(), unidade.getTelefone());
    }
}