package br.com.morbus.regulacao.domain.model;

import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Solicitacao {

    private UUID id;
    private UUID patientId;
    private UUID procedureId;
    private UUID unidadeSolicitanteId;
    private UUID unidadeExecutanteId;
    private UUID appointmentId;
    private EStatusSolicitacao status;
    private ERiscoSolicitado riskColor;
    private String cid;
    private String justificativaClinica;
    private String profissionalSolicitante;
    private String crmProfissional;
    private EDestino destino;
    private String justificativaNegacao;
    private UUID solicitadoPor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Solicitacao(UUID patientId,
                       UUID procedureId,
                       UUID unidadeSolicitanteId,
                       String cid,
                       String justificativaClinica,
                       String profissionalSolicitante,
                       String crmProfissional,
                       EDestino destino,
                       UUID solicitadoPor) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.procedureId = procedureId;
        this.unidadeSolicitanteId = unidadeSolicitanteId;
        this.status = EStatusSolicitacao.AGUARDANDO;
        this.riskColor = ERiscoSolicitado.AZUL;
        this.cid = cid;
        this.justificativaClinica = justificativaClinica;
        this.profissionalSolicitante = profissionalSolicitante;
        this.crmProfissional = crmProfissional;
        this.destino = destino;
        this.solicitadoPor = solicitadoPor;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Solicitacao(UUID id,
                       UUID patientId,
                       UUID procedureId,
                       UUID unidadeSolicitanteId,
                       UUID unidadeExecutanteId,
                       EStatusSolicitacao status,
                       ERiscoSolicitado riskColor,
                       String cid,
                       String justificativaClinica,
                       String profissionalSolicitante,
                       String crmProfissional,
                       EDestino destino,
                       String justificativaNegacao,
                       UUID solicitadoPor,
                       LocalDateTime createdAt,
                       LocalDateTime updatedAt,
                       UUID appointmentId) {
        this.id = id;
        this.patientId = patientId;
        this.procedureId = procedureId;
        this.unidadeSolicitanteId = unidadeSolicitanteId;
        this.unidadeExecutanteId = unidadeExecutanteId;
        this.status = status;
        this.riskColor = riskColor;
        this.cid = cid;
        this.justificativaClinica = justificativaClinica;
        this.profissionalSolicitante = profissionalSolicitante;
        this.crmProfissional = crmProfissional;
        this.destino = destino;
        this.justificativaNegacao = justificativaNegacao;
        this.solicitadoPor = solicitadoPor;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.appointmentId = appointmentId;
    }

    public void cancelar() {
        this.status = EStatusSolicitacao.CANCELADA;
    }

    public void aprovar(ERiscoSolicitado riskColor, UUID unidadeExecutanteId) {
        this.status = EStatusSolicitacao.APROVADA;
        this.riskColor = riskColor;
        this.unidadeExecutanteId = unidadeExecutanteId;
        this.updatedAt = LocalDateTime.now();
    }

    public void aprovarParaFilaEspera(UUID unidadeExecutanteId) {
        aprovar(ERiscoSolicitado.AZUL, unidadeExecutanteId);
        this.destino = EDestino.FILA_ESPERA;
    }

    public void negar(String justificativaNegacao) {
        this.status = EStatusSolicitacao.NEGADA;
        this.justificativaNegacao = justificativaNegacao;
        this.updatedAt = LocalDateTime.now();
    }

    public void devolver(String justificativaNegacao) {
        this.status = EStatusSolicitacao.DEVOLVIDA;
        this.justificativaNegacao = justificativaNegacao;
        this.updatedAt = LocalDateTime.now();
    }

    public void marcarPendente() {
        this.status = EStatusSolicitacao.PENDENTE;
        this.updatedAt = LocalDateTime.now();
    }

    public void agendar(UUID appointmentId) {
        this.status = EStatusSolicitacao.AGENDADA;
        this.appointmentId = appointmentId;
        this.updatedAt = LocalDateTime.now();
    }

    public void reclassificarRisco(ERiscoSolicitado novoRiskColor) {
        this.riskColor = novoRiskColor;
        this.updatedAt = LocalDateTime.now();
    }

    public void atender() {
        this.status = EStatusSolicitacao.ATENDIDA;
        this.updatedAt = LocalDateTime.now();
    }

    public void registrarFalta() {
        this.status = EStatusSolicitacao.FALTOU;
        this.updatedAt = LocalDateTime.now();
    }

    public void complementar(String cid, String justificativaClinica, String profissionalSolicitante, String crmProfissional) {
        if (cid != null) this.cid = cid;
        if (justificativaClinica != null) this.justificativaClinica = justificativaClinica;
        if (profissionalSolicitante != null) this.profissionalSolicitante = profissionalSolicitante;
        if (crmProfissional != null) this.crmProfissional = crmProfissional;
        this.status = EStatusSolicitacao.AGUARDANDO;
        this.justificativaNegacao = null;
        this.updatedAt = LocalDateTime.now();
    }
}
