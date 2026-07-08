package br.com.morbus.agendamento.domain.model;

import lombok.Getter;

import java.util.UUID;

@Getter
public class Provider {

    private final UUID id;
    private final String nome;
    private final String crm;
    private final String especialidade;

    public Provider(UUID id, String nome, String crm, String especialidade) {
        this.id = id;
        this.nome = nome;
        this.crm = crm;
        this.especialidade = especialidade;
    }
}
