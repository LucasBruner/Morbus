package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.application.command.CriarAgendamentoCommand;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.port.in.ICriarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;

import java.time.LocalDateTime;

public class CriarAgendamentoUseCase implements ICriarAgendamentoUseCase {

    private static final long EXPIRACAO_HORAS = 72;

    private final IAgendamentoRepository agendamentoRepository;

    public CriarAgendamentoUseCase(IAgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    @Override
    public Agendamento execute(CriarAgendamentoCommand command) {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(EXPIRACAO_HORAS);

        Agendamento agendamento = new Agendamento(
                command.queueEntryId(),
                command.slotId(),
                command.pacienteId(),
                expiresAt
        );

        return agendamentoRepository.save(agendamento);
    }
}
