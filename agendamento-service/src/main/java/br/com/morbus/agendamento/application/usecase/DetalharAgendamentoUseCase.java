package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.adapter.in.graphql.dto.AgendamentoDetalheRequestDTO;
import br.com.morbus.agendamento.adapter.security.UserPrincipal;
import br.com.morbus.agendamento.domain.model.Agendamento;
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
import org.springframework.security.core.Authentication;

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
    public Optional<AgendamentoDetalheRequestDTO> execute(UUID id, Authentication authentication) {
        Agendamento agendamento = agendamentoRepository.findById(id).orElse(null);

        if(!isAccessAllowed(authentication, agendamento)){
            return Optional.empty();
        }

        Slot slot = slotRepository.findById(agendamento.getSlotId());
        Schedule schedule = scheduleRepository.findById(slot.getScheduleId()).orElseThrow();
        HealthUnit unit = healthUnitRepository.findById(schedule.getUnitId()).orElseThrow();
        Provider provider = schedule.getProviderId() != null
                ? providerRepository.findById(schedule.getProviderId()).orElse(null)
                : null;

        return Optional.of(new AgendamentoDetalheRequestDTO(agendamento, slot, schedule, unit, provider));
    }

    private boolean isAccessAllowed(Authentication authentication, Agendamento agendamento) {
        if (authentication == null) {
            return true;
        }

        boolean isPaciente = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_PACIENTE".equals(a.getAuthority()));
        if (!isPaciente) {
            return true;
        }

        Object principal = authentication.getPrincipal();
        UUID jwtUserId;
        if (principal instanceof UserPrincipal userPrincipal) {
            jwtUserId = userPrincipal.userId();
        } else {
            try {
                jwtUserId = UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        return agendamento.getPacienteId().equals(jwtUserId);
    }
}
