package br.com.morbus.agendamento.domain.model;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Getter
public class Agendamento {

    private UUID id;
    private UUID queueEntryId;
    private UUID slotId;
    private UUID pacienteId;
    private EStatusAgendamento status;
    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime attendedAt;
    private LocalDateTime noShowAt;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Agendamento(UUID queueEntryId,
                       UUID slotId,
                       UUID pacienteId,
                       LocalDateTime expiresAt) {
        this(new AgendamentoSnapshot(
                UUID.randomUUID(),
                queueEntryId,
                slotId,
                pacienteId,
                EStatusAgendamento.AGUARDANDO_CONFIRMACAO,
                expiresAt,
                null,
                null,
                null,
                null,
                LocalDateTime.now(ZoneId.systemDefault()),
                null
        ));
    }

    public static Agendamento fromPersistence(AgendamentoSnapshot snapshot) {
        return new Agendamento(snapshot);
    }

    private Agendamento(AgendamentoSnapshot snapshot) {
        this.id = snapshot.id();
        this.queueEntryId = snapshot.queueEntryId();
        this.slotId = snapshot.slotId();
        this.pacienteId = snapshot.pacienteId();
        this.status = snapshot.status();
        this.expiresAt = snapshot.expiresAt();
        this.confirmedAt = snapshot.confirmedAt();
        this.attendedAt = snapshot.attendedAt();
        this.noShowAt = snapshot.noShowAt();
        this.cancellationReason = snapshot.cancellationReason();
        this.createdAt = snapshot.createdAt();
        this.updatedAt = snapshot.updatedAt();
    }

    public record AgendamentoSnapshot(UUID id,
                                      UUID queueEntryId,
                                      UUID slotId,
                                      UUID pacienteId,
                                      EStatusAgendamento status,
                                      LocalDateTime expiresAt,
                                      LocalDateTime confirmedAt,
                                      LocalDateTime attendedAt,
                                      LocalDateTime noShowAt,
                                      String cancellationReason,
                                      LocalDateTime createdAt,
                                      LocalDateTime updatedAt) {
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

    public void noShow() {
        this.status = EStatusAgendamento.FALTOU;
        this.noShowAt = LocalDateTime.now(ZoneId.systemDefault());
        this.updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    public void cancel(String motivo) {
        this.status = EStatusAgendamento.CANCELADO;
        this.cancellationReason = motivo;
        this.updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }
}
