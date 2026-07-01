package br.com.morbus.agendamento.application.usecase;

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
        if (agendamentoRepository.existsByPacienteIdAndSlotId(command.pacienteId(), command.slotId())) {
            throw new DuplicateAgendamentoException("Paciente já possui agendamento para o slot informado.");
        }

        Agendamento agendamento = new Agendamento(
                command.queueEntryId(),
                command.slotId(),
                command.pacienteId(),
                command.expiresAt()
        );

        return agendamentoRepository.save(agendamento);
    }
}
