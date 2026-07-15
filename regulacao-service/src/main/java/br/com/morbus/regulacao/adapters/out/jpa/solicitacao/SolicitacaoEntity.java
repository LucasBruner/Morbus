package br.com.morbus.regulacao.adapters.out.jpa.solicitacao;

import br.com.morbus.regulacao.domain.enums.EDestino;
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
    private UUID id;

    @Column(name = "paciente_id", nullable = false)
    private UUID patientId;

    @Column(name = "procedure_id", nullable = false)
    private UUID procedureId;

    @Column(name = "appointment_id")
    private UUID appointmentId;

    @Column(name = "unidade_solicitante_id", nullable = false)
    private UUID unidadeSolicitanteId;

    @Column(name = "unidade_executante_id")
    private UUID unidadeExecutanteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EStatusSolicitacao status;

    @Enumerated(EnumType.STRING)
    @Column(name = "risco_solicitado")
    private ERiscoSolicitado riskColor;

    @Column(name = "cid", length = 20)
    private String cid;

    @Column(name = "justificativa_clinica", columnDefinition = "TEXT")
    private String justificativaClinica;

    @Column(name = "profissional_solicitante", length = 200)
    private String profissionalSolicitante;

    @Column(name = "crm_profissional", length = 50)
    private String crmProfissional;

    @Enumerated(EnumType.STRING)
    @Column(name = "destino", length = 20)
    private EDestino destino;

    @Column(name = "justificativa_negacao", columnDefinition = "TEXT")
    private String justificativaNegacao;

    @Column(name = "solicitado_por", nullable = false)
    private UUID solicitadoPor;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Solicitacao toDomain() {
        return new Solicitacao(
                this.id,
                this.patientId,
                this.procedureId,
                this.unidadeSolicitanteId,
                this.unidadeExecutanteId,
                this.status,
                this.riskColor,
                this.cid,
                this.justificativaClinica,
                this.profissionalSolicitante,
                this.crmProfissional,
                this.destino,
                this.justificativaNegacao,
                this.solicitadoPor,
                this.createdAt,
                this.updatedAt,
                this.appointmentId,
                this.observacoes
        );
    }

    public static SolicitacaoEntity fromDomain(Solicitacao s) {
        SolicitacaoEntity entity = new SolicitacaoEntity();
        entity.id = s.getId();
        entity.patientId = s.getPatientId();
        entity.procedureId = s.getProcedureId();
        entity.unidadeSolicitanteId = s.getUnidadeSolicitanteId();
        entity.unidadeExecutanteId = s.getUnidadeExecutanteId();
        entity.status = s.getStatus();
        entity.riskColor = s.getRiskColor();
        entity.cid = s.getCid();
        entity.justificativaClinica = s.getJustificativaClinica();
        entity.profissionalSolicitante = s.getProfissionalSolicitante();
        entity.crmProfissional = s.getCrmProfissional();
        entity.destino = s.getDestino();
        entity.justificativaNegacao = s.getJustificativaNegacao();
        entity.solicitadoPor = s.getSolicitadoPor();
        entity.createdAt = s.getCreatedAt();
        entity.updatedAt = s.getUpdatedAt();
        entity.appointmentId = s.getAppointmentId();
        entity.observacoes = s.getObservacoes();
        return entity;
    }
}
