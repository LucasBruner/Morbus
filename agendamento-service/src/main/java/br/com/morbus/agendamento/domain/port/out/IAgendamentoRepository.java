package br.com.morbus.agendamento.domain.port.out;

import br.com.morbus.agendamento.domain.model.Agendamento;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IAgendamentoRepository {

    Agendamento save(Agendamento agendamento);

    Optional<Agendamento> findByPacienteIdAndDataHora(UUID pacienteId, LocalDateTime dataHora);
}
