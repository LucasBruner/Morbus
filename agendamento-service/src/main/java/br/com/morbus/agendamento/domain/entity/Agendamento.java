package br.com.morbus.agendamento.domain.entity;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Agendamento {

    private final UUID id;
    private final UUID pacienteId;
    private final UUID procedimentoId;
    private final UUID unidadeId;
    private final LocalDateTime dataHora;
    private final EStatusAgendamento status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Agendamento(UUID pacienteId,
                       UUID procedimentoId,
                       UUID unidadeId,
                       LocalDateTime dataHora) {
        this(
                UUID.randomUUID(),
                pacienteId,
                procedimentoId,
                unidadeId,
                dataHora,
                EStatusAgendamento.AGENDADO,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    public Agendamento(UUID id,
                       UUID pacienteId,
                       UUID procedimentoId,
                       UUID unidadeId,
                       LocalDateTime dataHora,
                       EStatusAgendamento status,
                       LocalDateTime createdAt,
                       LocalDateTime updatedAt) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.procedimentoId = procedimentoId;
        this.unidadeId = unidadeId;
        this.dataHora = dataHora;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
