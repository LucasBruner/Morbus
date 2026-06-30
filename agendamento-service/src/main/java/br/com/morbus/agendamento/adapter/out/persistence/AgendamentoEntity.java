package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments", schema = "agendamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "queue_entry_id", nullable = false)
    private UUID queueEntryId;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(name = "paciente_id", nullable = false)
    private UUID pacienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EStatusAgendamento status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
