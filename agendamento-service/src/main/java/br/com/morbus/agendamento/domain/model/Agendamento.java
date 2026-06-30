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
    private final EStatusAgendamento status;
    private final LocalDateTime expiresAt;
    private final String motivoCancelamento;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

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
                       String motivoCancelamento,
                       LocalDateTime createdAt,
                       LocalDateTime updatedAt) {
        this.id = id;
        this.queueEntryId = queueEntryId;
        this.slotId = slotId;
        this.pacienteId = pacienteId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.motivoCancelamento = motivoCancelamento;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
