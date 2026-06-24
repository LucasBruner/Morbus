package br.com.morbus.agendamento.domain;

import br.com.morbus.agendamento.application.command.CriarAgendamentoCommand;
import br.com.morbus.agendamento.domain.exception.DuplicateAgendamentoException;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.port.in.ICriarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;

public class CriarAgendamentoUseCase implements ICriarAgendamentoUseCase {

    private final IAgendamentoRepository agendamentoRepository;

    public CriarAgendamentoUseCase(IAgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    @Override
    public Agendamento execute(CriarAgendamentoCommand command) {
        agendamentoRepository.findByPacienteIdAndDataHora(command.pacienteId(), command.dataHora())
                .ifPresent(agendamento -> {
                    throw new DuplicateAgendamentoException("Paciente ja possui agendamento para o horario informado.");
                });

        Agendamento agendamento = new Agendamento(
                command.pacienteId(),
                command.procedimentoId(),
                command.unidadeId(),
                command.dataHora()
        );

        return agendamentoRepository.save(agendamento);
    }
}
