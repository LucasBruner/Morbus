package br.com.morbus.agendamento.adapter.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
public class AgendamentoController {

    @PatchMapping("/{id}/confirmar")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<Void> confirmar(@PathVariable UUID id) {
        return ResponseEntity.status(501).build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<Void> cancelar(
            @PathVariable UUID id,
            @RequestParam(required = false) String motivo) {
        return ResponseEntity.status(501).build();
    }

    @PatchMapping("/{id}/attend")
    @PreAuthorize("hasRole('EXECUTANTE')")
    public ResponseEntity<Void> registrarPresenca(@PathVariable UUID id) {
        return ResponseEntity.status(501).build();
    }

    @PostMapping("/{id}/falta")
    @PreAuthorize("hasRole('EXECUTANTE')")
    public ResponseEntity<Void> registrarFalta(@PathVariable UUID id) {
        return ResponseEntity.status(501).build();
    }
}
