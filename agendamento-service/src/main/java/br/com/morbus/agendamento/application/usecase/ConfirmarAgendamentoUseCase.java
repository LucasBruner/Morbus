package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.application.command.ConfirmarAgendamentoResult;
import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.exception.AgendamentoNotFoundException;
import br.com.morbus.agendamento.domain.exception.ExpiredConfirmationException;
import br.com.morbus.agendamento.domain.exception.InvalidAgendamentoStatusException;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.IConfirmarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class ConfirmarAgendamentoUseCase implements IConfirmarAgendamentoUseCase {

    private final IAgendamentoRepository agendamentoRepository;
    private final ISlotRepository slotRepository;

    public ConfirmarAgendamentoUseCase(IAgendamentoRepository agendamentoRepository, ISlotRepository slotRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.slotRepository = slotRepository;
    }

    @Override
    @Transactional
    public ConfirmarAgendamentoResult execute(UUID agendamentoId, UUID userId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new AgendamentoNotFoundException("Agendamento nao encontrado: " + agendamentoId));

        if (!agendamento.getPacienteId().equals(userId)) {
            throw new AccessDeniedException("Operacao restrita ao paciente dono do agendamento");
        }

        if (!EStatusAgendamento.AGUARDANDO_CONFIRMACAO.equals(agendamento.getStatus())) {
            throw new InvalidAgendamentoStatusException("Agendamento nao esta aguardando confirmacao");
        }

        if (LocalDateTime.now(ZoneId.systemDefault()).isAfter(agendamento.getExpiresAt())) {
            throw new ExpiredConfirmationException("Prazo de confirmacao expirado");
        }

        agendamento.confirm();
        Agendamento agendamentoSalvo = agendamentoRepository.save(agendamento);
        Slot slot = slotRepository.findById(agendamentoSalvo.getSlotId());

        return new ConfirmarAgendamentoResult(
                agendamentoSalvo.getId(),
                agendamentoSalvo.getStatus(),
                agendamentoSalvo.getConfirmedAt(),
                slot.getDataHora());
    }
}
