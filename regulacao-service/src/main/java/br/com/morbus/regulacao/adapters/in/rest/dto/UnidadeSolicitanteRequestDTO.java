package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.ports.in.dto.CadastrarUnidadeSolicitanteCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnidadeSolicitanteRequestDTO(
        @NotBlank(message = "cnes é obrigatório") @Size(max = 7, message = "cnes deve ter no maximo 7 caracteres") String cnes,
        @NotBlank(message = "nome é obrigatório") String nome,
        String endereco,
        String telefone) {

    public CadastrarUnidadeSolicitanteCommand toCommand() {
        return new CadastrarUnidadeSolicitanteCommand(cnes, nome, endereco, telefone);
    }
}