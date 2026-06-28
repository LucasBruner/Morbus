package br.com.morbus.regulacao.domain.model;

import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Solicitacao {

    private UUID id;
    private UUID pacienteId;
    private UUID procedureId;
    private UUID unidadeSolicitanteId;
    private UUID unidadeExecutanteId;
    private EStatusSolicitacao status;
    private ERiscoSolicitado riscoSolicitado;
    private String observacoes;
    private String justificativaNegacao;
    private UUID solicitadoPor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Solicitacao(UUID pacienteId,
                       UUID procedureId,
                       UUID unidadeSolicitanteId,
                       ERiscoSolicitado riscoSolicitado,
                       String observacoes,
                       UUID solicitadoPor) {
        this.id = UUID.randomUUID();
        this.pacienteId = pacienteId;
        this.procedureId = procedureId;
        this.unidadeSolicitanteId = unidadeSolicitanteId;
        this.status = EStatusSolicitacao.PENDENTE;
        this.riscoSolicitado = riscoSolicitado;
        this.observacoes = observacoes;
        this.solicitadoPor = solicitadoPor;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Solicitacao(UUID id,
                       UUID pacienteId,
                       UUID procedureId,
                       UUID unidadeSolicitanteId,
                       UUID unidadeExecutanteId,
                       EStatusSolicitacao status,
                       ERiscoSolicitado riscoSolicitado,
                       String observacoes,
                       String justificativaNegacao,
                       UUID solicitadoPor,
                       LocalDateTime createdAt,
                       LocalDateTime updatedAt) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.procedureId = procedureId;
        this.unidadeSolicitanteId = unidadeSolicitanteId;
        this.unidadeExecutanteId = unidadeExecutanteId;
        this.status = status;
        this.riscoSolicitado = riscoSolicitado;
        this.observacoes = observacoes;
        this.justificativaNegacao = justificativaNegacao;
        this.solicitadoPor = solicitadoPor;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void changeStatus (EStatusSolicitacao status) {
        this.status = status;
    }
}
