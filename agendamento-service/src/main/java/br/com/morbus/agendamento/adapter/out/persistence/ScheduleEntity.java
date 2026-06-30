package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.enums.ETurnos;
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
@Table(name = "schedules", schema = "agendamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDateTime dataFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "turno", nullable = false)
    private ETurnos turno;
}
