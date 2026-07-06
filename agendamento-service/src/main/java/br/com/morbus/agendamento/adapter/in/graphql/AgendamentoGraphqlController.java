package br.com.morbus.agendamento.adapter.in.graphql;

import br.com.morbus.agendamento.adapter.in.graphql.dto.SlotsAvaiableResponseDTO;
import br.com.morbus.agendamento.domain.port.in.IConsultarDisponibilidadeUseCase;
import jakarta.validation.constraints.NotNull;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class AgendamentoGraphqlController {

	private final IConsultarDisponibilidadeUseCase consultarDisponibilidadeUseCase;

	public AgendamentoGraphqlController(IConsultarDisponibilidadeUseCase consultarDisponibilidadeUseCase) {
		this.consultarDisponibilidadeUseCase = consultarDisponibilidadeUseCase;
	}

	@QueryMapping
	@PreAuthorize("hasAnyAuthority('ROLE_REGULADOR','ROLE_MEDICO','ROLE_PACIENTE','ROLE_EXECUTANTE')")
	public List<SlotsAvaiableResponseDTO> disponibilidade(@Argument @NotNull UUID procedureId,
                                                          @Argument UUID unitId,
                                                          @Argument @NotNull String dateFrom,
                                                          @Argument @NotNull String dateTo) {
		return consultarDisponibilidadeUseCase.execute(procedureId, unitId, dateFrom, dateTo)
				.stream()
				.map(SlotsAvaiableResponseDTO::fromEntity)
				.toList();
	}
}
