package br.com.morbus.regulacao.adapters.out.jpa.unidade;

import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Entity
@Table(name = "unidades_solicitantes", schema = "regulacao")
@Getter
@Setter
@NoArgsConstructor
public class UnidadeSolicitanteEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false, length = 7, unique = true)
    private String cnes;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(length = 255)
    private String endereco;

    @Column(length = 20)
    private String telefone;

    @Transient
    private boolean isNew = true;

    public UnidadeSolicitanteEntity(UUID id, String cnes, String nome, String endereco, String telefone) {
        this.id = id;
        this.cnes = cnes;
        this.nome = nome;
        this.endereco = endereco;
        this.telefone = telefone;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public UnidadeSolicitante toDomain() {
        return new UnidadeSolicitante(this.id, this.cnes, this.nome, this.endereco, this.telefone);
    }

    public static UnidadeSolicitanteEntity fromDomain(UnidadeSolicitante unidade) {
        return new UnidadeSolicitanteEntity(unidade.getId(), unidade.getCnes(), unidade.getNome(),
                unidade.getEndereco(), unidade.getTelefone());
    }
}