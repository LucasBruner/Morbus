package br.com.morbus.agendamento.adapter.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    @PostMapping
    @PreAuthorize("hasRole('EXECUTANTE')")
    public ResponseEntity<Void> criar(@RequestBody Object request) {
        return ResponseEntity.status(501).build();
    }

    @PostMapping("/{id}/bloquear")
    @PreAuthorize("hasRole('EXECUTANTE')")
    public ResponseEntity<Void> bloquear(
            @PathVariable UUID id,
            @RequestBody Object request) {
        return ResponseEntity.status(501).build();
    }
}
