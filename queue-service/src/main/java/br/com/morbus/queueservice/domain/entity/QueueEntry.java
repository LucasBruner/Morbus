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
    private int posicaoCalculada;

    public Integer calculatePriorityScore() {
        return 1;
    }

    public void call() {
        this.queueStatus = EQueueStatus.AGENDADO;
        this.updatedAt = LocalDateTime.now();
    }
}
