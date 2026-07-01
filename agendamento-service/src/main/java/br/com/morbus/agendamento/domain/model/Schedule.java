package br.com.morbus.agendamento.domain.model;

import br.com.morbus.agendamento.domain.enums.EDiaSemana;
import lombok.Getter;

import java.time.LocalTime;
import java.util.UUID;

@Getter
public class Schedule {

    private final UUID id;
    private final UUID unitId;
    private final UUID providerId;
    private final UUID procedureId;
    private final EDiaSemana diaDaSemana;
    private final LocalTime horarioInicio;
    private final LocalTime horarioFim;
    private final int slotDuracaoMinutos;
    private final int capacidade;
    private final boolean ativo;

    public Schedule(UUID unitId,
                    UUID providerId,
                    UUID procedureId,
                    EDiaSemana diaDaSemana,
                    LocalTime horarioInicio,
                    LocalTime horarioFim,
                    int slotDuracaoMinutos,
                    int capacidade) {
        this(UUID.randomUUID(), unitId, providerId, procedureId,
                diaDaSemana, horarioInicio, horarioFim, slotDuracaoMinutos, capacidade, true);
    }

    public Schedule(UUID id,
                    UUID unitId,
                    UUID providerId,
                    UUID procedureId,
                    EDiaSemana diaDaSemana,
                    LocalTime horarioInicio,
                    LocalTime horarioFim,
                    int slotDuracaoMinutos,
                    int capacidade,
                    boolean ativo) {
        this.id = id;
        this.unitId = unitId;
        this.providerId = providerId;
        this.procedureId = procedureId;
        this.diaDaSemana = diaDaSemana;
        this.horarioInicio = horarioInicio;
        this.horarioFim = horarioFim;
        this.slotDuracaoMinutos = slotDuracaoMinutos;
        this.capacidade = capacidade;
        this.ativo = ativo;
    }
}
