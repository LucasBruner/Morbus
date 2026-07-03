package br.com.morbus.regulacao.ports.in.dto;

public record CadastrarUnidadeSolicitanteCommand(String cnes, String nome, String endereco, String telefone) {
}