package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.exception.InvalidSchedulePeriodException;
import br.com.morbus.agendamento.domain.exception.ScheduleNotFoundException;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.port.in.IAtualizarScheduleUseCase;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalTime;
import java.util.UUID;

public class AtualizarScheduleUseCase implements IAtualizarScheduleUseCase {

    private final IScheduleRepository scheduleRepository;

    public AtualizarScheduleUseCase(IScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public Schedule execute(UUID id,
                            UUID requesterUnitId,
                            UUID providerId,
                            LocalTime horarioInicio,
                            LocalTime horarioFim,
                            Integer capacidade) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ScheduleNotFoundException("Grade nao encontrada: " + id));

        if (!schedule.getUnitId().equals(requesterUnitId)) {
            throw new AccessDeniedException("EXECUTANTE restrito a sua unidade.");
        }

        if (!horarioFim.isAfter(horarioInicio)) {
            throw new InvalidSchedulePeriodException("horarioFim deve ser maior que horarioInicio.");
        }

        Schedule atualizada = new Schedule(
                schedule.getId(),
                schedule.getUnitId(),
                providerId,
                schedule.getProcedureId(),
                schedule.getDiaDaSemana(),
                horarioInicio,
                horarioFim,
                schedule.getSlotDuracaoMinutos(),
                capacidade,
                schedule.isAtivo()
        );

        return scheduleRepository.save(atualizada);
    }
}
