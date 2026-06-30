package br.com.morbus.agendamento.adapter.in.rest;

import br.com.morbus.agendamento.adapter.in.rest.dto.ScheduleCreatedResponseDTO;
import br.com.morbus.agendamento.adapter.in.rest.dto.ScheduleRequestDTO;
import br.com.morbus.agendamento.adapter.security.UserPrincipal;
import br.com.morbus.agendamento.application.command.CriarScheduleResult;
import br.com.morbus.agendamento.domain.port.in.ICriarScheduleUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ICriarScheduleUseCase criarScheduleUseCase;

    public ScheduleController(ICriarScheduleUseCase criarScheduleUseCase) {
        this.criarScheduleUseCase = criarScheduleUseCase;
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
}
