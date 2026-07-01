package br.com.morbus.agendamento.domain.port.out;

import br.com.morbus.agendamento.domain.model.Agendamento;

import java.util.Optional;
import java.util.UUID;

public interface IAgendamentoRepository {

    Agendamento save(Agendamento agendamento);
    Optional<Agendamento> findById(UUID id);
    boolean existsByPacienteIdAndSlotId(UUID pacienteId, UUID slotId);
}
