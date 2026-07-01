package br.com.morbus.agendamento.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IAgendamentoJpaRepository extends JpaRepository<AgendamentoEntity, UUID> {

    boolean existsByPacienteIdAndSlotId(UUID pacienteId, UUID slotId);
}
