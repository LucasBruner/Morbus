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

    public HealthUnit(UUID id, String nome, String cnes, String municipio, String uf) {
        this.id = id;
        this.nome = nome;
        this.cnes = cnes;
        this.municipio = municipio;
        this.uf = uf;
    }
}
