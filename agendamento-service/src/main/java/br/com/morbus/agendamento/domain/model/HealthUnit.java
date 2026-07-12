package br.com.morbus.agendamento.domain.model;

import lombok.Getter;

import java.util.UUID;

@Getter
public class HealthUnit {

    private final UUID id;
    private final String nome;
    private final String cnes;
    private final String municipio;
    private final String uf;
    private final String endereco;
    private final String telefone;

    public HealthUnit(UUID id, String nome, String cnes, String municipio, String uf) {
        this(id, nome, cnes, municipio, uf, null, null);
    }

    public HealthUnit(UUID id, String nome, String cnes, String municipio, String uf, String endereco) {
        this(id, nome, cnes, municipio, uf, endereco, null);
    }

    public HealthUnit(UUID id, String nome, String cnes, String municipio, String uf, String endereco, String telefone) {
        this.id = id;
        this.nome = nome;
        this.cnes = cnes;
        this.municipio = municipio;
        this.uf = uf;
        this.endereco = endereco;
        this.telefone = telefone;
    }
}
