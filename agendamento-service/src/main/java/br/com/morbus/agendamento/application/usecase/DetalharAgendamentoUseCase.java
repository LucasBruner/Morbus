package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.exception.AgendamentoNotFoundException;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.model.AgendamentoComDetalhes;
import br.com.morbus.agendamento.domain.model.HealthUnit;
import br.com.morbus.agendamento.domain.model.Provider;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.IDetalharAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import br.com.morbus.agendamento.domain.port.out.IHealthUnitRepository;
import br.com.morbus.agendamento.domain.port.out.IProviderRepository;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;

import java.util.Optional;
import java.util.UUID;

public class DetalharAgendamentoUseCase implements IDetalharAgendamentoUseCase {

    private final IAgendamentoRepository agendamentoRepository;
    private final ISlotRepository slotRepository;
    private final IScheduleRepository scheduleRepository;
    private final IHealthUnitRepository healthUnitRepository;
    private final IProviderRepository providerRepository;

    public DetalharAgendamentoUseCase(IAgendamentoRepository agendamentoRepository,
                                      ISlotRepository slotRepository,
                                      IScheduleRepository scheduleRepository,
                                      IHealthUnitRepository healthUnitRepository,
                                      IProviderRepository providerRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.slotRepository = slotRepository;
        this.scheduleRepository = scheduleRepository;
        this.healthUnitRepository = healthUnitRepository;
        this.providerRepository = providerRepository;
    }

    @Override
    public Optional<AgendamentoComDetalhes> execute(UUID id, UUID requesterId, String requesterRole) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new AgendamentoNotFoundException("Agendamento nao encontrado: " + id));

        if(!isAccessAllowed(requesterId, requesterRole, agendamento)){
            return Optional.empty();
        }

        Slot slot = slotRepository.findById(agendamento.getSlotId());
        Schedule schedule = scheduleRepository.findById(slot.getScheduleId()).orElseThrow();
        HealthUnit unit = healthUnitRepository.findById(schedule.getUnitId()).orElseThrow();
        Provider provider = schedule.getProviderId() != null
                ? providerRepository.findById(schedule.getProviderId()).orElse(null)
                : null;

        return Optional.of(new AgendamentoComDetalhes(agendamento, slot, schedule, unit, provider));
    }

    private boolean isAccessAllowed(UUID requesterId, String requesterRole, Agendamento agendamento) {
        if (requesterId == null) {
            return true;
        }

        boolean isPaciente = "ROLE_PACIENTE".equals(requesterRole);
        if (!isPaciente) {
            return true;
        }

        return agendamento.getPacienteId().equals(requesterId);
    }
}
