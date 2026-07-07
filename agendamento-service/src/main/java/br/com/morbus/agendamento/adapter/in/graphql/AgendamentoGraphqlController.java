package br.com.morbus.agendamento.adapter.in.graphql;

import br.com.morbus.agendamento.adapter.in.graphql.dto.AgendamentosPacienteResponseDTO;
import br.com.morbus.agendamento.adapter.in.graphql.dto.SlotsAvailableResponseDTO;
import br.com.morbus.agendamento.adapter.security.UserPrincipal;
import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.port.in.IAgendamentosPacienteUseCase;
import br.com.morbus.agendamento.domain.port.in.IConsultarDisponibilidadeUseCase;
import jakarta.validation.constraints.NotNull;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class AgendamentoGraphqlController {

	private final IConsultarDisponibilidadeUseCase consultarDisponibilidadeUseCase;
    private final IAgendamentosPacienteUseCase agendamentosPacienteUseCase;

	public AgendamentoGraphqlController(IConsultarDisponibilidadeUseCase consultarDisponibilidadeUseCase, IAgendamentosPacienteUseCase agendamentosPacienteUseCase) {
		this.consultarDisponibilidadeUseCase = consultarDisponibilidadeUseCase;
        this.agendamentosPacienteUseCase = agendamentosPacienteUseCase;
    }

	@QueryMapping
	@PreAuthorize("hasAnyAuthority('ROLE_REGULADOR','ROLE_MEDICO','ROLE_PACIENTE','ROLE_EXECUTANTE')")
	public List<SlotsAvailableResponseDTO> disponibilidadeSlots(@Argument @NotNull UUID procedureId,
                                                                @Argument UUID unitId,
                                                                @Argument @NotNull String dateFrom,
                                                                @Argument @NotNull String dateTo) {
		return consultarDisponibilidadeUseCase.execute(procedureId, unitId, dateFrom, dateTo)
				.stream()
				.map(SlotsAvailableResponseDTO::fromEntity)
				.toList();
	}

    @QueryMapping
    @PreAuthorize("hasAnyAuthority('ROLE_REGULADOR','ROLE_MEDICO','ROLE_PACIENTE','ROLE_EXECUTANTE')")
    public List<AgendamentosPacienteResponseDTO> agendamentosPaciente(@Argument UUID patientId,
                                                                      @Argument UUID unitId,
                                                                      @Argument EStatusAgendamento status,
                                                                      @Argument @NotNull String dateFrom,
                                                                      @Argument @NotNull String dateTo) {
        UUID effectivePatientId = resolvePatientId(patientId);

        return agendamentosPacienteUseCase.execute(effectivePatientId, unitId, status, dateFrom, dateTo)
                .stream()
                .map(AgendamentosPacienteResponseDTO::fromEntity)
                .toList();
    }

    private UUID resolvePatientId(UUID requestedPatientId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return requestedPatientId;
        }

        boolean isPaciente = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_PACIENTE".equals(authority.getAuthority()));

        if (!isPaciente) {
            return requestedPatientId;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.userId();
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Nao foi possivel identificar o paciente autenticado no token.", ex);
        }
    }
}
