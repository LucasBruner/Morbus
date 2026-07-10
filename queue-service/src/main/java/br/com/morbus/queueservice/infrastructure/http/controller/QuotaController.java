package br.com.morbus.queueservice.infrastructure.http.controller;

import br.com.morbus.queueservice.domain.entity.UnitProcedureQuota;
import br.com.morbus.queueservice.domain.usecase.CreateOrUpdateQuota;
import br.com.morbus.queueservice.domain.usecase.GetQuota;
import br.com.morbus.queueservice.domain.usecase.dto.QuotaRequestDTO;
import br.com.morbus.queueservice.domain.usecase.dto.QuotaResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quotas")
@Tag(name = "Quotas", description = "Endpoints para gerenciamento de cotas de FILA_ESPERA por unidade+procedimento")
@PreAuthorize("hasRole('MEDICO')")
public class QuotaController {

    private final CreateOrUpdateQuota createOrUpdateQuota;
    private final GetQuota getQuota;

    public QuotaController(CreateOrUpdateQuota createOrUpdateQuota, GetQuota getQuota) {
        this.createOrUpdateQuota = createOrUpdateQuota;
        this.getQuota = getQuota;
    }

    @PostMapping
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Cria ou atualiza a cota diária de uma unidade+procedimento",
            description = "FILA_ESPERA passa a ser limitada a maxPerDay entradas AGUARDANDO por dia para essa combinação. FILA_REGULADA nunca é afetada.")
    public ResponseEntity<QuotaResponseDTO> createOrUpdate(@RequestBody @Valid QuotaRequestDTO request) {
        UnitProcedureQuota quota = createOrUpdateQuota.execute(request.unitId(), request.procedureId(), request.maxPerDay());
        return ResponseEntity.status(HttpStatus.CREATED).body(QuotaResponseDTO.fromEntity(quota));
    }

    @GetMapping
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(summary = "Consulta a cota configurada para uma unidade+procedimento")
    public ResponseEntity<QuotaResponseDTO> getByUnitAndProcedure(@RequestParam UUID unitId,
                                                                   @RequestParam UUID procedureId) {
        UnitProcedureQuota quota = getQuota.execute(unitId, procedureId);
        return ResponseEntity.ok(QuotaResponseDTO.fromEntity(quota));
    }
}
