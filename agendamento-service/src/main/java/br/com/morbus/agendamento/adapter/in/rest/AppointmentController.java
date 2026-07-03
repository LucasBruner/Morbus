package br.com.morbus.agendamento.adapter.in.rest;

import br.com.morbus.agendamento.adapter.security.UserPrincipal;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.port.in.IConfirmarAgendamentoUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final IConfirmarAgendamentoUseCase confirmarAgendamentoUseCase;

    public AppointmentController(IConfirmarAgendamentoUseCase confirmarAgendamentoUseCase) {
        this.confirmarAgendamentoUseCase = confirmarAgendamentoUseCase;
    }

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasAuthority('ROLE_PACIENTE')")
    public ResponseEntity<?> confirmar(@PathVariable("id") UUID id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        Agendamento confirmado = confirmarAgendamentoUseCase.execute(id, principal.userId());
        return ResponseEntity.ok().build();
    }
}
