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
    public ResponseEntity<ScheduleUpdatedResponseDTO> atualizar(@PathVariable UUID id,
                                                                 @Valid @RequestBody AtualizarScheduleRequestDTO request,
                                                                 @AuthenticationPrincipal UserPrincipal principal) {
        Schedule schedule = atualizarScheduleUseCase.execute(id, principal.unitId(), request.providerId(),
                request.horarioInicio(), request.horarioFim(), request.capacidade());
        return ResponseEntity.ok(ScheduleUpdatedResponseDTO.fromDomain(schedule));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('EXECUTANTE')")
    public ResponseEntity<Void> block(@PathVariable UUID id,
                                      @Valid @RequestBody BlockRequestDTO request,
                                      @AuthenticationPrincipal UserPrincipal principal) {

        blockSlotUseCase.execute(id, principal.unitId(), request.date(), request.motivo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unblock")
    @PreAuthorize("hasRole('EXECUTANTE')")
    public ResponseEntity<Void> unblock(@PathVariable UUID id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        unblockSlotUseCase.execute(id, principal.unitId());
        return ResponseEntity.noContent().build();
    }
}
