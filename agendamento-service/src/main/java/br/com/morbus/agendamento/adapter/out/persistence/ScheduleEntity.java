package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.enums.EDiaSemana;
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

import java.time.LocalTime;
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

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "provider_id")
    private UUID providerId;

    @Column(name = "procedure_id")
    private UUID procedureId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_da_semana", nullable = false)
    private EDiaSemana diaDaSemana;

    @Column(name = "horario_inicio", nullable = false)
    private LocalTime horarioInicio;

    @Column(name = "horario_fim", nullable = false)
    private LocalTime horarioFim;

    @Column(name = "slot_duracao_minutos", nullable = false)
    private Integer slotDuracaoMinutos;

    @Column(name = "capacidade", nullable = false)
    private Integer capacidade;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;
}
