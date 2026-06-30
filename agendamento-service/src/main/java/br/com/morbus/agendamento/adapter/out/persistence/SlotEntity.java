package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.enums.EStatusSlots;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "slots", schema = "agendamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlotEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "schedule_id", nullable = false)
    private UUID scheduleId;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "capacidade", nullable = false)
    private Integer capacidade;

    @Column(name = "reservados", nullable = false)
    private Integer reservados;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EStatusSlots status;
}
