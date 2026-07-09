package br.com.morbus.agendamento.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "providers", schema = "agendamento")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProviderEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "crm", nullable = false)
    private String crm;

    @Column(name = "especialidade", nullable = false)
    private String especialidade;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;
}
