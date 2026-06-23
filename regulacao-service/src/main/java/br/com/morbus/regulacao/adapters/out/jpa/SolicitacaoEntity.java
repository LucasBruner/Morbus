package br.com.morbus.regulacao.adapters.out.jpa;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "solicitacoes", schema = "regulacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolicitacaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "paciente_id", nullable = false)
    private UUID pacienteId;

    @Column(name = "procedure_id", nullable = false)
    private UUID procedureId;

    @Column(name = "unidade_solicitante_id", nullable = false)
    private UUID unidadeSolicitanteId;

    @Column(name = "unidade_executante_id")
    private UUID unidadeExecutanteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EStatusSolicitacao status;

    @Enumerated(EnumType.STRING)
    @Column(name = "risco_solicitado")
    private ERiscoSolicitado riscoSolicitado;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "justificativa_negacao", columnDefinition = "TEXT")
    private String justificativaNegacao;

    @Column(name = "solicitado_por", nullable = false)
    private UUID solicitadoPor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Solicitacao toDomain() {
        return new Solicitacao(
                this.id,
                this.pacienteId,
                this.procedureId,
                this.unidadeSolicitanteId,
                this.unidadeExecutanteId,
                this.status,
                this.riscoSolicitado,
                this.observacoes,
                this.justificativaNegacao,
                this.solicitadoPor,
                this.createdAt,
                this.updatedAt
        );
    }

    public static SolicitacaoEntity fromDomain(Solicitacao s) {
        SolicitacaoEntity entity = new SolicitacaoEntity();
        entity.id = s.getId();
        entity.pacienteId = s.getPacienteId();
        entity.procedureId = s.getProcedureId();
        entity.unidadeSolicitanteId = s.getUnidadeSolicitanteId();
        entity.unidadeExecutanteId = s.getUnidadeExecutanteId();
        entity.status = s.getStatus();
        entity.riscoSolicitado = s.getRiscoSolicitado();
        entity.observacoes = s.getObservacoes();
        entity.justificativaNegacao = s.getJustificativaNegacao();
        entity.solicitadoPor = s.getSolicitadoPor();
        entity.createdAt = s.getCreatedAt();
        entity.updatedAt = s.getUpdatedAt();
        return entity;
    }
}
