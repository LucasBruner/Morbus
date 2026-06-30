package br.com.morbus.agendamento.domain.model;

import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Slot {

    private final UUID id;
    private final UUID scheduleId;
    private final LocalDateTime dataHora;
    private final int capacidade;
    private final int reservados;
    private final EStatusSlots status;

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
}
