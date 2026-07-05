package br.com.morbus.agendamento.adapter.in.rest;

import br.com.morbus.agendamento.adapter.in.rest.dto.AgendamentoAttendResponseDTO;
import br.com.morbus.agendamento.adapter.in.rest.dto.AgendamentoNoShowResponseDTO;
import br.com.morbus.agendamento.adapter.security.UserPrincipal;
import br.com.morbus.agendamento.adapter.in.rest.dto.ConfirmarAgendamentoResponseDTO;
import br.com.morbus.agendamento.application.command.ConfirmarAgendamentoResult;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.port.in.IAtenderAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.ICancelarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.IConfirmarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.IRegistrarFaltaAgendamentoUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final IConfirmarAgendamentoUseCase confirmarAgendamentoUseCase;
    private final IAtenderAgendamentoUseCase atenderAgendamentoUseCase;
    private final IRegistrarFaltaAgendamentoUseCase registrarFaltaAgendamentoUseCase;
    private final ICancelarAgendamentoUseCase cancelarAgendamentoUseCase;

    public AppointmentController(IConfirmarAgendamentoUseCase confirmarAgendamentoUseCase,
                                 IAtenderAgendamentoUseCase atenderAgendamentoUseCase,
                                 IRegistrarFaltaAgendamentoUseCase registrarFaltaAgendamentoUseCase,
                                 ICancelarAgendamentoUseCase cancelarAgendamentoUseCase) {
        this.confirmarAgendamentoUseCase = confirmarAgendamentoUseCase;
        this.atenderAgendamentoUseCase = atenderAgendamentoUseCase;
        this.registrarFaltaAgendamentoUseCase = registrarFaltaAgendamentoUseCase;
        this.cancelarAgendamentoUseCase = cancelarAgendamentoUseCase;
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    public ResponseEntity<ConfirmarAgendamentoResponseDTO> confirmar(@PathVariable("id") UUID id,
                                                                     @AuthenticationPrincipal UserPrincipal principal) {
        ConfirmarAgendamentoResult result = confirmarAgendamentoUseCase.execute(id, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ConfirmarAgendamentoResponseDTO.fromResult(result));
    }

    @PatchMapping("/{id}/attend")
    @PreAuthorize("hasAuthority('ROLE_EXECUTANTE')")
    public ResponseEntity<AgendamentoAttendResponseDTO> attend(@PathVariable("id") UUID id,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        Agendamento agendamento = atenderAgendamentoUseCase.execute(id, principal.unitId());
        return ResponseEntity.ok(AgendamentoAttendResponseDTO.fromEntity(agendamento));
    }

    @PostMapping("/{id}/falta")
    @PreAuthorize("hasAuthority('ROLE_EXECUTANTE')")
    public ResponseEntity<AgendamentoNoShowResponseDTO> falta(@PathVariable("id") UUID id,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        Agendamento agendamento = registrarFaltaAgendamentoUseCase.execute(id, principal.unitId());
        return ResponseEntity.ok(AgendamentoNoShowResponseDTO.fromEntity(agendamento));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_PACIENTE','ROLE_MEDICO')")
    public ResponseEntity<Void> cancelar(@PathVariable("id") UUID id,
                                         @RequestParam(value = "motivo", required = false) String motivo,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        cancelarAgendamentoUseCase.execute(id, principal.userId(), principal.role(), motivo);
        return ResponseEntity.noContent().build();
    }
}
