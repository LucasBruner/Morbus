package br.com.morbus.agendamento.adapter.in.rest;

import br.com.morbus.agendamento.adapter.in.rest.dto.AtualizarScheduleRequestDTO;
import br.com.morbus.agendamento.adapter.in.rest.dto.BlockRequestDTO;
import br.com.morbus.agendamento.adapter.in.rest.dto.ScheduleCreatedResponseDTO;
import br.com.morbus.agendamento.adapter.in.rest.dto.ScheduleRequestDTO;
import br.com.morbus.agendamento.adapter.in.rest.dto.ScheduleUpdatedResponseDTO;
import br.com.morbus.agendamento.adapter.security.UserPrincipal;
import br.com.morbus.agendamento.application.command.CriarScheduleResult;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.port.in.IAtualizarScheduleUseCase;
import br.com.morbus.agendamento.domain.port.in.IBlockSlotUseCase;
import br.com.morbus.agendamento.domain.port.in.ICriarScheduleUseCase;
import br.com.morbus.agendamento.domain.port.in.IUnblockSlotUseCase;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
@Tag(name = "Schedules", description = "Endpoints para gerenciamento de grades de atendimento (schedules) e bloqueio de slots")
public class ScheduleController {

    private final ICriarScheduleUseCase criarScheduleUseCase;
    private final IUnblockSlotUseCase unblockSlotUseCase;
    private final IBlockSlotUseCase blockSlotUseCase;
    private final IAtualizarScheduleUseCase atualizarScheduleUseCase;

    public ScheduleController(ICriarScheduleUseCase criarScheduleUseCase,
                              IUnblockSlotUseCase unblockSlotUseCase,
                              IBlockSlotUseCase blockSlotUseCase,
                              IAtualizarScheduleUseCase atualizarScheduleUseCase) {
        this.criarScheduleUseCase = criarScheduleUseCase;
        this.unblockSlotUseCase = unblockSlotUseCase;
        this.blockSlotUseCase = blockSlotUseCase;
        this.atualizarScheduleUseCase = atualizarScheduleUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('EXECUTANTE')")
    @Operation(
            summary = "Cria uma grade de atendimento",
            description = "Cria uma nova grade (schedule) para um provider em uma unidade e dia da semana, gerando automaticamente os slots do periodo informado. O EXECUTANTE so pode criar grades para a propria unidade.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ScheduleRequestDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "providerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "unitId": "7b3c1a2d-9e4f-4a8b-b6d1-1f2e3a4b5c6d",
                                      "procedureId": "9c1e2d3f-4a5b-6c7d-8e9f-0a1b2c3d4e5f",
                                      "diaDaSemana": "SEGUNDA",
                                      "horarioInicio": "08:00:00",
                                      "horarioFim": "12:00:00",
                                      "slotDuracaoMinutos": 30,
                                      "capacidade": 1
                                    }"""))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Grade criada com sucesso e slots gerados",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ScheduleCreatedResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada invalidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao (requer ROLE_EXECUTANTE) ou tentativa de criar grade para outra unidade", content = @Content),
            @ApiResponse(responseCode = "422", description = "Horario final nao e maior que o inicial ou ja existe grade ativa para o provider, unidade e dia informados", content = @Content)
    })
    public ResponseEntity<ScheduleCreatedResponseDTO> criar(
            @Valid @RequestBody ScheduleRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.unitId() == null || !principal.unitId().equals(request.unitId())) {
            throw new AccessDeniedException("EXECUTANTE restrito a sua unidade.");
        }

        CriarScheduleResult result = criarScheduleUseCase.execute(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ScheduleCreatedResponseDTO.fromResult(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EXECUTANTE')")
    @Operation(
            summary = "Atualiza uma grade de atendimento",
            description = "Atualiza o provider, horario e capacidade de uma grade existente. Restrito a unidade do EXECUTANTE autenticado.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AtualizarScheduleRequestDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "providerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "horarioInicio": "08:00:00",
                                      "horarioFim": "13:00:00",
                                      "capacidade": 2
                                    }"""))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grade atualizada com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ScheduleUpdatedResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada invalidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao (requer ROLE_EXECUTANTE) ou grade nao pertence a unidade do usuario", content = @Content),
            @ApiResponse(responseCode = "404", description = "Grade nao encontrada", content = @Content)
    })
    public ResponseEntity<ScheduleUpdatedResponseDTO> atualizar(
            @Parameter(description = "ID da grade") @PathVariable UUID id,
            @Valid @RequestBody AtualizarScheduleRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Schedule schedule = atualizarScheduleUseCase.execute(id, principal.unitId(), request.providerId(),
                request.horarioInicio(), request.horarioFim(), request.capacidade());
        return ResponseEntity.ok(ScheduleUpdatedResponseDTO.fromDomain(schedule));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('EXECUTANTE')")
    @Operation(
            summary = "Bloqueia slots de uma grade em uma data",
            description = "Bloqueia todos os slots disponiveis de uma grade em uma data especifica, impedindo novos agendamentos (ex: feriado, ausencia do provider).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BlockRequestDTO.class),
                            examples = @ExampleObject(value = """
                                    { "date": "2026-07-20", "motivo": "Feriado municipal" }""")
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Slots bloqueados com sucesso", content = @Content),
            @ApiResponse(responseCode = "400", description = "Dados de entrada invalidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao (requer ROLE_EXECUTANTE) ou grade nao pertence a unidade do usuario", content = @Content),
            @ApiResponse(responseCode = "404", description = "Grade nao encontrada ou nenhum slot disponivel na data informada", content = @Content)
    })
    public ResponseEntity<Void> block(
            @Parameter(description = "ID da grade") @PathVariable UUID id,
            @Valid @RequestBody BlockRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {

        blockSlotUseCase.execute(id, principal.unitId(), request.date(), request.motivo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unblock")
    @PreAuthorize("hasRole('EXECUTANTE')")
    @Operation(
            summary = "Desbloqueia slots de uma grade",
            description = "Reverte o bloqueio dos slots indisponiveis de uma grade, tornando-os disponiveis novamente para agendamento.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Slots desbloqueados com sucesso", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao (requer ROLE_EXECUTANTE) ou grade nao pertence a unidade do usuario", content = @Content),
            @ApiResponse(responseCode = "404", description = "Grade nao encontrada ou nenhum slot bloqueado para desbloquear", content = @Content)
    })
    public ResponseEntity<Void> unblock(
            @Parameter(description = "ID da grade") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        unblockSlotUseCase.execute(id, principal.unitId());
        return ResponseEntity.noContent().build();
    }
}
