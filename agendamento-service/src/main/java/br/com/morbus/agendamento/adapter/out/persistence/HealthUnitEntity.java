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
@Table(name = "health_units", schema = "agendamento")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HealthUnitEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "cnes", nullable = false)
    private String cnes;

    @Column(name = "municipio", nullable = false)
    private String municipio;

    @Column(name = "uf", nullable = false)
    private String uf;

    @Column(name = "endereco")
    private String endereco;

    @Column(name = "telefone")
    private String telefone;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;
}
