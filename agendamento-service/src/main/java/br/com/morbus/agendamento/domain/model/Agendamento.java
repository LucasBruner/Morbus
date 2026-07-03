package br.com.morbus.agendamento.domain.model;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Agendamento {

    private final UUID id;
    private final UUID queueEntryId;
    private final UUID slotId;
    private final UUID pacienteId;
    private EStatusAgendamento status;
    private final LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;
    private final String cancellationReason;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Agendamento(UUID queueEntryId,
                       UUID slotId,
                       UUID pacienteId,
                       LocalDateTime expiresAt) {
        this(
                UUID.randomUUID(),
                queueEntryId,
                slotId,
                pacienteId,
                EStatusAgendamento.AGUARDANDO_CONFIRMACAO,
                expiresAt,
                null,
                LocalDateTime.now(),
                null
        );
    }

    public Agendamento(UUID id,
                       UUID queueEntryId,
                       UUID slotId,
                       UUID pacienteId,
                       EStatusAgendamento status,
                       LocalDateTime expiresAt,
                       String cancellationReason,
                       LocalDateTime createdAt,
                       LocalDateTime updatedAt) {
        this.id = id;
        this.queueEntryId = queueEntryId;
        this.slotId = slotId;
        this.pacienteId = pacienteId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.confirmedAt = null;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Agendamento(UUID id,
                       UUID queueEntryId,
                       UUID slotId,
                       UUID pacienteId,
                       EStatusAgendamento status,
                       LocalDateTime expiresAt,
                       LocalDateTime confirmedAt,
                       String cancellationReason,
                       LocalDateTime createdAt,
                       LocalDateTime updatedAt) {
        this.id = id;
        this.queueEntryId = queueEntryId;
        this.slotId = slotId;
        this.pacienteId = pacienteId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.confirmedAt = confirmedAt;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void confirm() {
        this.status = EStatusAgendamento.CONFIRMADO;
        this.confirmedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
