package br.com.morbus.agendamento.domain.model;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private LocalDateTime attendedAt;
    private String cancellationReason;
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
                null,
                null,
                LocalDateTime.now(ZoneId.systemDefault()),
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
        this(id, queueEntryId, slotId, pacienteId, status, expiresAt, null, null, cancellationReason, createdAt, updatedAt);
    }

    public Agendamento(UUID id,
                       UUID queueEntryId,
                       UUID slotId,
                       UUID pacienteId,
                       EStatusAgendamento status,
                       LocalDateTime expiresAt,
                       LocalDateTime confirmedAt,
                       LocalDateTime attendedAt,
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
        this.attendedAt = attendedAt;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void confirm() {
        this.status = EStatusAgendamento.CONFIRMADO;
        this.confirmedAt = LocalDateTime.now(ZoneId.systemDefault());
        this.updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    public void attend() {
        this.status = EStatusAgendamento.ATENDIDO;
        this.attendedAt = LocalDateTime.now(ZoneId.systemDefault());
        this.updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    public void cancel(String motivo) {
        this.status = EStatusAgendamento.CANCELADO;
        this.cancellationReason = motivo;
        this.updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }
}
