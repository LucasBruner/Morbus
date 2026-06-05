package br.com.morbus.queueservice.domain.entity;

import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
public class QueueEntry {
    private UUID id;
    private Patient patient;
    private Procedure procedure;
    private ERiskColor riskColor;
    private EQueueStatus queueStatus;
    private LocalDateTime registeredAt;
    private LocalDateTime updatedAt;
    private int scorePosicaoCalculada;

    public void calculatePriorityScore() {
        int colorScore = this.riskColor != null
                ? this.riskColor.getNumericPriority()
                : 4;

        int groupScore = (this.patient != null && this.patient.getGrupoLegal() != null)
                ? this.patient.getGrupoLegal().getNumericPriority()
                : 6;

        this.scorePosicaoCalculada = colorScore * 10 + groupScore;
    }

    public void call() {
        this.queueStatus = EQueueStatus.AGENDADO;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateRiskColor(ERiskColor riskColor) {
        this.riskColor = riskColor;
        this.updatedAt = LocalDateTime.now();
        calculatePriorityScore();
    }
}
