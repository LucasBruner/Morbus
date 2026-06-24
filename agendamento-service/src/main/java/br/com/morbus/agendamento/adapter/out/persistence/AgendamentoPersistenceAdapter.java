package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AgendamentoPersistenceAdapter implements IAgendamentoRepository {

    @Override
    public Agendamento save(Agendamento agendamento) {
        return agendamento;
    }

    @Override
    public Optional<Agendamento> findByPacienteIdAndDataHora(UUID pacienteId, LocalDateTime dataHora) {
        return Optional.empty();
    }
}
