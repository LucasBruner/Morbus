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
    private final Integer duracaoMinutos;
    private final EStatusSlots status;

    public Slot(UUID scheduleId,
                LocalDateTime dataHora,
                Integer duracaoMinutos) {
        this(UUID.randomUUID(), scheduleId, dataHora, duracaoMinutos, EStatusSlots.DISPONIVEL);
    }

    public Slot(UUID id,
                UUID scheduleId,
                LocalDateTime dataHora,
                Integer duracaoMinutos,
                EStatusSlots status) {
        this.id = id;
        this.scheduleId = scheduleId;
        this.dataHora = dataHora;
        this.duracaoMinutos = duracaoMinutos;
        this.status = status;
    }
}
