package br.com.morbus.agendamento.adapter.in.rest;

import br.com.morbus.agendamento.adapter.in.rest.dto.AgendamentoAttendResponseDTO;
import br.com.morbus.agendamento.adapter.in.rest.dto.ReagendarAgendamentoRequestDTO;
import br.com.morbus.agendamento.adapter.in.rest.dto.ReagendarAgendamentoResponseDTO;
import br.com.morbus.agendamento.adapter.security.UserPrincipal;
import br.com.morbus.agendamento.adapter.in.rest.dto.ConfirmarAgendamentoResponseDTO;
import br.com.morbus.agendamento.application.command.ConfirmarAgendamentoResult;
import br.com.morbus.agendamento.application.command.ReagendarAgendamentoResult;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.port.in.IAtenderAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.ICancelarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.IConfirmarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.IReagendarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.IRegistrarFaltaAgendamentoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Endpoints para gerenciamento do ciclo de vida dos agendamentos")
public class AppointmentController {

    private final IConfirmarAgendamentoUseCase confirmarAgendamentoUseCase;
    private final IAtenderAgendamentoUseCase atenderAgendamentoUseCase;
    private final IRegistrarFaltaAgendamentoUseCase registrarFaltaAgendamentoUseCase;
    private final ICancelarAgendamentoUseCase cancelarAgendamentoUseCase;
    private final IReagendarAgendamentoUseCase reagendarAgendamentoUseCase;

    public AppointmentController(IConfirmarAgendamentoUseCase confirmarAgendamentoUseCase,
                                 IAtenderAgendamentoUseCase atenderAgendamentoUseCase,
                                 IRegistrarFaltaAgendamentoUseCase registrarFaltaAgendamentoUseCase,
                                 ICancelarAgendamentoUseCase cancelarAgendamentoUseCase,
                                 IReagendarAgendamentoUseCase reagendarAgendamentoUseCase) {
        this.confirmarAgendamentoUseCase = confirmarAgendamentoUseCase;
        this.atenderAgendamentoUseCase = atenderAgendamentoUseCase;
        this.registrarFaltaAgendamentoUseCase = registrarFaltaAgendamentoUseCase;
        this.cancelarAgendamentoUseCase = cancelarAgendamentoUseCase;
        this.reagendarAgendamentoUseCase = reagendarAgendamentoUseCase;
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    @Operation(
            summary = "Confirma presenca no agendamento",
            description = "Permite que o paciente dono do agendamento confirme sua presenca antes do prazo de expiracao.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento confirmado com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConfirmarAgendamentoResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": "a1b2c3d4-e5f6-7890-ab12-cd34ef567890",
                                      "status": "CONFIRMADO",
                                      "confirmedAt": "2026-07-12T10:30:00",
                                      "slotDateTime": "2026-07-15T14:00:00",
                                      "unitName": "UBS Central",
                                      "unitAddress": "Rua das Flores, 100"
                                    }"""))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao (requer ROLE_PACIENTE) ou agendamento pertence a outro paciente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Agendamento nao encontrado", content = @Content),
            @ApiResponse(responseCode = "422", description = "Agendamento nao esta aguardando confirmacao ou o prazo de confirmacao expirou", content = @Content)
    })
    public ResponseEntity<ConfirmarAgendamentoResponseDTO> confirmar(
            @Parameter(description = "ID do agendamento") @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ConfirmarAgendamentoResult result = confirmarAgendamentoUseCase.execute(id, principal.userId());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ConfirmarAgendamentoResponseDTO.fromResult(result));
    }

    @PatchMapping("/{id}/attend")
    @PreAuthorize("hasAuthority('ROLE_EXECUTANTE')")
    @Operation(
            summary = "Registra atendimento do agendamento",
            description = "Marca o agendamento como atendido pela unidade executante responsavel pelo slot.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atendimento registrado com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AgendamentoAttendResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": "a1b2c3d4-e5f6-7890-ab12-cd34ef567890",
                                      "status": "ATENDIDO",
                                      "attendedAt": "2026-07-15T14:05:00"
                                    }"""))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao (requer ROLE_EXECUTANTE) ou unidade nao responsavel pelo agendamento", content = @Content),
            @ApiResponse(responseCode = "404", description = "Agendamento nao encontrado", content = @Content),
            @ApiResponse(responseCode = "422", description = "Status do agendamento nao permite registro de atendimento", content = @Content)
    })
    public ResponseEntity<AgendamentoAttendResponseDTO> attend(
            @Parameter(description = "ID do agendamento") @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Agendamento agendamento = atenderAgendamentoUseCase.execute(id, principal.unitId());
        return ResponseEntity.ok(AgendamentoAttendResponseDTO.fromEntity(agendamento));
    }

    @PostMapping("/{id}/falta")
    @PreAuthorize("hasAuthority('ROLE_EXECUTANTE')")
    @Operation(
            summary = "Registra falta do paciente",
            description = "Marca o agendamento como falta (no-show) e libera o slot para nova reserva.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Falta registrada com sucesso", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao (requer ROLE_EXECUTANTE) ou unidade nao responsavel pelo agendamento", content = @Content),
            @ApiResponse(responseCode = "404", description = "Agendamento nao encontrado", content = @Content),
            @ApiResponse(responseCode = "422", description = "Status do agendamento nao permite registro de falta", content = @Content)
    })
    public ResponseEntity<Void> falta(
            @Parameter(description = "ID do agendamento") @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        registrarFaltaAgendamentoUseCase.execute(id, principal.unitId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_PACIENTE','ROLE_MEDICO')")
    @Operation(
            summary = "Cancela agendamento",
            description = "Cancela um agendamento, liberando o slot associado. Pacientes so podem cancelar os proprios agendamentos; medicos podem cancelar qualquer agendamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Agendamento cancelado com sucesso", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao ou paciente nao e dono do agendamento", content = @Content),
            @ApiResponse(responseCode = "404", description = "Agendamento nao encontrado", content = @Content),
            @ApiResponse(responseCode = "422", description = "Status do agendamento nao permite cancelamento ou prazo de atendimento ja expirou", content = @Content)
    })
    public ResponseEntity<Void> cancelar(
            @Parameter(description = "ID do agendamento") @PathVariable("id") UUID id,
            @Parameter(description = "Motivo do cancelamento") @RequestParam(value = "motivo", required = false) String motivo,
            @AuthenticationPrincipal UserPrincipal principal) {
        cancelarAgendamentoUseCase.execute(id, principal.userId(), principal.role(), motivo);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reagendar")
    @PreAuthorize("hasAuthority('ROLE_MEDICO')")
    @Operation(
            summary = "Reagenda o agendamento para outro slot",
            description = "Move o agendamento para um novo slot disponivel, liberando o slot anterior.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ReagendarAgendamentoRequestDTO.class),
                            examples = @ExampleObject(value = """
                                    { "newSlotId": "7b3c1a2d-9e4f-4a8b-b6d1-1f2e3a4b5c6d" }""")
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento reagendado com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReagendarAgendamentoResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": "a1b2c3d4-e5f6-7890-ab12-cd34ef567890",
                                      "status": "CONFIRMADO",
                                      "slotId": "7b3c1a2d-9e4f-4a8b-b6d1-1f2e3a4b5c6d",
                                      "slotDate": "2026-07-16T09:00:00"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada invalidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao (requer ROLE_MEDICO)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Agendamento nao encontrado", content = @Content),
            @ApiResponse(responseCode = "422", description = "Status do agendamento nao permite reagendamento ou novo slot indisponivel", content = @Content)
    })
    public ResponseEntity<ReagendarAgendamentoResponseDTO> reagendar(
            @Parameter(description = "ID do agendamento") @PathVariable("id") UUID id,
            @Valid @RequestBody ReagendarAgendamentoRequestDTO request) {
        ReagendarAgendamentoResult result = reagendarAgendamentoUseCase.execute(id, request.newSlotId());
        return ResponseEntity.ok(ReagendarAgendamentoResponseDTO.fromResult(result));
    }
}
