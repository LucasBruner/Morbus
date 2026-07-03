package br.com.morbus.agendamento.adapter.in.rest;

import br.com.morbus.agendamento.adapter.security.UserPrincipal;
import br.com.morbus.agendamento.domain.port.in.ICancelarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.IConfirmarAgendamentoUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final IConfirmarAgendamentoUseCase confirmarAgendamentoUseCase;
    private final ICancelarAgendamentoUseCase cancelarAgendamentoUseCase;

    public AppointmentController(IConfirmarAgendamentoUseCase confirmarAgendamentoUseCase,
                                 ICancelarAgendamentoUseCase cancelarAgendamentoUseCase) {
        this.confirmarAgendamentoUseCase = confirmarAgendamentoUseCase;
        this.cancelarAgendamentoUseCase = cancelarAgendamentoUseCase;
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    public ResponseEntity<Void> confirmar(@PathVariable("id") UUID id,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        confirmarAgendamentoUseCase.execute(id, principal.userId());
        return ResponseEntity.ok().build();
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
