package br.com.morbus.regulacao.adapters.out.jpa.parecer;

import br.com.morbus.regulacao.domain.enums.EDecisaoRegulador;
import br.com.morbus.regulacao.domain.model.Parecer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pareceres", schema = "regulacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParecerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "solicitacao_id", nullable = false)
    private UUID solicitacaoId;

    @Column(name = "regulador_id", nullable = false)
    private UUID reguladorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EDecisaoRegulador decisao;

    @Column(columnDefinition = "TEXT")
    private String justificativa;

    @Column(name = "emitido_em", nullable = false, updatable = false)
    private LocalDateTime emitidoEm;

    public Parecer toDomain() {
        return new Parecer(this.id, this.solicitacaoId, this.reguladorId, this.decisao, this.justificativa, this.emitidoEm);
    }

    public static ParecerEntity fromDomain(Parecer p) {
        ParecerEntity entity = new ParecerEntity();
        entity.id = p.getId();
        entity.solicitacaoId = p.getSolicitacaoId();
        entity.reguladorId = p.getReguladorId();
        entity.decisao = p.getDecisao();
        entity.justificativa = p.getJustificativa();
        entity.emitidoEm = p.getEmitidoEm();
        return entity;
    }
}
