package br.com.morbus.agendamento.adapter.in.graphql;

import br.com.morbus.agendamento.adapter.in.graphql.dto.AgendamentoDetalheResponseDTO;
import br.com.morbus.agendamento.adapter.in.graphql.dto.AgendamentosPacienteResponseDTO;
import br.com.morbus.agendamento.adapter.in.graphql.dto.ScheduleResponseDTO;
import br.com.morbus.agendamento.adapter.in.graphql.dto.SlotsAvailableResponseDTO;
import br.com.morbus.agendamento.adapter.security.UserPrincipal;
import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.port.in.IAgendamentosPacienteUseCase;
import br.com.morbus.agendamento.domain.port.in.IConsultarDisponibilidadeUseCase;
import br.com.morbus.agendamento.domain.port.in.IConsultarGradeUseCase;
import br.com.morbus.agendamento.domain.port.in.IDetalharAgendamentoUseCase;
import jakarta.validation.constraints.NotNull;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Controller
@Validated
public class AgendamentoGraphqlController {

	private final IConsultarDisponibilidadeUseCase consultarDisponibilidadeUseCase;
    private final IAgendamentosPacienteUseCase agendamentosPacienteUseCase;
    private final IDetalharAgendamentoUseCase detalharAgendamentoUseCase;
    private final IConsultarGradeUseCase consultarGradeUseCase;

	public AgendamentoGraphqlController(IConsultarDisponibilidadeUseCase consultarDisponibilidadeUseCase,
                                        IAgendamentosPacienteUseCase agendamentosPacienteUseCase,
                                        IDetalharAgendamentoUseCase detalharAgendamentoUseCase,
                                        IConsultarGradeUseCase consultarGradeUseCase) {
		this.consultarDisponibilidadeUseCase = consultarDisponibilidadeUseCase;
        this.agendamentosPacienteUseCase = agendamentosPacienteUseCase;
        this.detalharAgendamentoUseCase = detalharAgendamentoUseCase;
        this.consultarGradeUseCase = consultarGradeUseCase;
    }

    @QueryMapping(name = "disponibilidade")
	@PreAuthorize("hasAnyAuthority('ROLE_REGULADOR','ROLE_MEDICO','ROLE_PACIENTE','ROLE_EXECUTANTE')")
	public List<SlotsAvailableResponseDTO> disponibilidade(@Argument @NotNull UUID procedureId,
                                                                @Argument UUID unitId,
                                                                @Argument @NotNull String dateFrom,
                                                                @Argument @NotNull String dateTo) {
		return consultarDisponibilidadeUseCase.execute(procedureId, unitId, dateFrom, dateTo)
				.stream()
				.map(SlotsAvailableResponseDTO::fromEntity)
				.toList();
	}

    @QueryMapping(name = "agendamentos")
    @PreAuthorize("hasAnyAuthority('ROLE_REGULADOR','ROLE_MEDICO','ROLE_PACIENTE','ROLE_EXECUTANTE')")
    public List<AgendamentosPacienteResponseDTO> agendamentos(@Argument UUID patientId,
                                                                      @Argument UUID unitId,
                                                                      @Argument EStatusAgendamento status,
                                                                      @Argument @NotNull String dateFrom,
                                                                      @Argument @NotNull String dateTo) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return agendamentosPacienteUseCase.execute(patientId, unitId, status, dateFrom, dateTo,
                        resolveRequesterId(authentication), resolveRequesterRole(authentication))
                .stream()
                .map(AgendamentosPacienteResponseDTO::fromDetalhe)
                .toList();
    }

    @QueryMapping(name = "agendamento")
    @PreAuthorize("hasAnyAuthority('ROLE_REGULADOR','ROLE_MEDICO','ROLE_PACIENTE','ROLE_EXECUTANTE')")
    public AgendamentoDetalheResponseDTO agendamento(@Argument @NotNull UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return detalharAgendamentoUseCase.execute(id,
                        resolveRequesterId(authentication), resolveRequesterRole(authentication))
                .map(AgendamentoDetalheResponseDTO::fromDetalhe)
                .orElse(null);
    }

    private UUID resolveRequesterId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.userId();
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String resolveRequesterRole(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.role();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .findFirst()
                .orElse(null);
    }

    @QueryMapping(name = "grade")
    @PreAuthorize("hasAnyAuthority('ROLE_EXECUTANTE','ROLE_MEDICO','ROLE_REGULADOR')")
    public List<ScheduleResponseDTO> grade(@Argument @NotNull UUID unitId,
                                           @Argument @NotNull String week) {
        return consultarGradeUseCase.execute(unitId, week)
                .stream()
                .map(ScheduleResponseDTO::fromGradeItem)
                .toList();
    }
}
