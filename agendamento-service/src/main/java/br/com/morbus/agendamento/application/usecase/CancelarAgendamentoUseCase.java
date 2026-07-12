package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.adapter.out.rabbitmq.IAgendamentoEventPublisher;
import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.exception.AgendamentoNotFoundException;
import br.com.morbus.agendamento.domain.exception.CancelamentoNaoPermitidoException;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.ICancelarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class CancelarAgendamentoUseCase implements ICancelarAgendamentoUseCase {

    private final IAgendamentoRepository agendamentoRepository;
    private final ISlotRepository slotRepository;
    private final IAgendamentoEventPublisher eventPublisher;

    public CancelarAgendamentoUseCase(IAgendamentoRepository agendamentoRepository,
                                      ISlotRepository slotRepository,
                                      IAgendamentoEventPublisher eventPublisher) {
        this.agendamentoRepository = agendamentoRepository;
        this.slotRepository = slotRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void execute(UUID agendamentoId, UUID requesterId, String requesterRole, String motivo) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new AgendamentoNotFoundException("Agendamento nao encontrado: " + agendamentoId));

        if (requesterRole.equals("ROLE_PACIENTE") && !agendamento.getPacienteId().equals(requesterId)) {
            throw new AccessDeniedException("Operacao restrita ao paciente dono do agendamento");
        }

        if (EStatusAgendamento.ATENDIDO.equals(agendamento.getStatus())
                || EStatusAgendamento.FALTOU.equals(agendamento.getStatus())) {
            throw new CancelamentoNaoPermitidoException("Agendamento nao pode ser cancelado apos atendimento");
        }

        if (!(EStatusAgendamento.AGUARDANDO_CONFIRMACAO.equals(agendamento.getStatus())
                || EStatusAgendamento.CONFIRMADO.equals(agendamento.getStatus()))) {
            throw new CancelamentoNaoPermitidoException("Status nao permitido para cancelamento");
        }

        if (LocalDateTime.now(ZoneId.systemDefault()).isAfter(agendamento.getExpiresAt())) {
            throw new CancelamentoNaoPermitidoException("Cancelamento apenas antes da data do atendimento");
        }

        agendamento.cancel(motivo);

        Slot slot = slotRepository.findById(agendamento.getSlotId());
        slot.releaseOne();
        slotRepository.save(slot);

        Agendamento saved = agendamentoRepository.save(agendamento);

        eventPublisher.publishAppointmentCancelled(
                saved.getId(),
                saved.getQueueEntryId(),
                saved.getPacienteId(),
                motivo,
                LocalDateTime.now(ZoneId.systemDefault())
        );
    }
}
