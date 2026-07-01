package br.com.morbus.agendamento.adapter.in.rest;

import br.com.morbus.agendamento.application.command.AlterarSlotStatusResult;
import br.com.morbus.agendamento.domain.port.in.IBlockSlotUseCase;
import br.com.morbus.agendamento.domain.port.in.IUnblockSlotUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/slots")
public class SlotController {

    private final IUnblockSlotUseCase unblockSlotUseCase;
    private final IBlockSlotUseCase blockSlotUseCase;

    public SlotController(IUnblockSlotUseCase unblockSlotUseCase, IBlockSlotUseCase blockSlotUseCase) {
        this.unblockSlotUseCase = unblockSlotUseCase;
        this.blockSlotUseCase = blockSlotUseCase;
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('EXECUTANTE')")
    public ResponseEntity<AlterarSlotStatusResult> block(@PathVariable UUID id) {
        return ResponseEntity.ok(blockSlotUseCase.execute(id));
    }

    @PatchMapping("/{id}/unblock")
    @PreAuthorize("hasRole('EXECUTANTE')")
    public ResponseEntity<AlterarSlotStatusResult> unblock(@PathVariable UUID id) {
        return ResponseEntity.ok(unblockSlotUseCase.execute(id));
    }
}
