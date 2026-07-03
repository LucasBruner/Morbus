package br.com.morbus.agendamento.domain.model;

import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Slot {

    private UUID id;
    private UUID scheduleId;
    private LocalDateTime dataHora;
    private int capacidade;
    private int reservados;
    private EStatusSlots status;

    public Slot(UUID scheduleId,
                LocalDateTime dataHora,
                int capacidade) {
        this(UUID.randomUUID(), scheduleId, dataHora, capacidade, 0, EStatusSlots.DISPONIVEL);
    }

    public Slot(UUID id,
                UUID scheduleId,
                LocalDateTime dataHora,
                int capacidade,
                int reservados,
                EStatusSlots status) {
        this.id = id;
        this.scheduleId = scheduleId;
        this.dataHora = dataHora;
        this.capacidade = capacidade;
        this.reservados = reservados;
        this.status = status;
    }

    public void block() {
        this.status = EStatusSlots.INDISPONIVEL;
    }

    public void unblock() {
        this.status = EStatusSlots.DISPONIVEL;
    }

    public void reserveOne() {
        this.reservados += 1;
        this.status = this.reservados >= this.capacidade
                ? EStatusSlots.OCUPADO
                : EStatusSlots.DISPONIVEL;
    }
}
