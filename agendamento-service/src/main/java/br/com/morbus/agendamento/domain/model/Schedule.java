package br.com.morbus.agendamento.domain.model;

import br.com.morbus.agendamento.domain.enums.ETurnos;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Schedule {

    private final UUID id;
    private final UUID providerId;
    private final UUID unitId;
    private final LocalDateTime dataInicio;
    private final LocalDateTime dataFim;
    private final ETurnos turno;

    public Schedule(UUID providerId,
                    UUID unitId,
                    LocalDateTime dataInicio,
                    LocalDateTime dataFim,
                    ETurnos turno) {
        this(UUID.randomUUID(), providerId, unitId, dataInicio, dataFim, turno);
    }

    public Schedule(UUID id,
                    UUID providerId,
                    UUID unitId,
                    LocalDateTime dataInicio,
                    LocalDateTime dataFim,
                    ETurnos turno) {
        this.id = id;
        this.providerId = providerId;
        this.unitId = unitId;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.turno = turno;
    }
}
