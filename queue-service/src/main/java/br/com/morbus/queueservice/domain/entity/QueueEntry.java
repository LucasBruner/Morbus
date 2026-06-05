package br.com.morbus.queueservice.domain.entity;

import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}
