package br.com.morbus.regulacao.domain.model;

import lombok.Getter;

import java.util.UUID;

@Getter
public class UnidadeSolicitante {

    private final UUID id;
    private final String cnes;
    private final String nome;
    private final String endereco;
    private final String telefone;

    public UnidadeSolicitante(String cnes, String nome, String endereco, String telefone) {
        this(UUID.randomUUID(), cnes, nome, endereco, telefone);
    }

    public UnidadeSolicitante(UUID id, String cnes, String nome, String endereco, String telefone) {
        this.id = id;
        this.cnes = cnes;
        this.nome = nome;
        this.endereco = endereco;
        this.telefone = telefone;
    }
}