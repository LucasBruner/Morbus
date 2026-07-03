package br.com.morbus.regulacao.adapters.out.jpa.unidade;

import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "unidades_solicitantes", schema = "regulacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnidadeSolicitanteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 7, unique = true)
    private String cnes;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(length = 255)
    private String endereco;

    @Column(length = 20)
    private String telefone;

    public UnidadeSolicitante toDomain() {
        return new UnidadeSolicitante(this.id, this.cnes, this.nome, this.endereco, this.telefone);
    }

    public static UnidadeSolicitanteEntity fromDomain(UnidadeSolicitante unidade) {
        return new UnidadeSolicitanteEntity(unidade.getId(), unidade.getCnes(), unidade.getNome(),
                unidade.getEndereco(), unidade.getTelefone());
    }
}