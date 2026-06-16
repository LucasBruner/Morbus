package br.com.morbus.queueservice.infrastructure.http.controller;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.usecase.CallNextPatient;
import br.com.morbus.queueservice.domain.usecase.CancelQueueEntry;
import br.com.morbus.queueservice.domain.usecase.GetQueuePosition;
import br.com.morbus.queueservice.domain.usecase.ListQueueByPriority;
import br.com.morbus.queueservice.domain.usecase.ReclassifyPriority;
import br.com.morbus.queueservice.domain.usecase.RegisterPatientInQueue;
import br.com.morbus.queueservice.domain.usecase.dto.CancelQueueRequestDTO;
import br.com.morbus.queueservice.domain.usecase.dto.ListQueueByPriorityDTO;
import br.com.morbus.queueservice.domain.usecase.dto.QueueCancelDTO;
import br.com.morbus.queueservice.domain.usecase.dto.QueueEntryResponseDTO;
import br.com.morbus.queueservice.domain.usecase.dto.QueueEntryRiskQueuePosition;
import br.com.morbus.queueservice.domain.usecase.dto.QueuePositionResponseDTO;
import br.com.morbus.queueservice.domain.usecase.dto.QueueUpdateRiskColorDTO;
import br.com.morbus.queueservice.domain.usecase.dto.ReclassifyPriorityRequestDTO;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterQueueRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/queue")
@Tag(name = "Queue", description = "Endpoints para gerenciamento de filas")
public class QueueController {

    private final RegisterPatientInQueue registerPatientInQueue;
    private final ListQueueByPriority listQueueByPriority;
    private final GetQueuePosition getQueuePosition;
    private final CallNextPatient callNextPatient;
    private final ReclassifyPriority reclassifyPriority;
    private final CancelQueueEntry cancelQueueEntry;

    public QueueController(RegisterPatientInQueue registerPatientInQueue,
                           ListQueueByPriority listQueueByPriority,
                           GetQueuePosition getQueuePosition,
                           CallNextPatient callNextPatient,
                           ReclassifyPriority reclassifyPriority,
                           CancelQueueEntry cancelQueueEntry) {
        this.registerPatientInQueue = registerPatientInQueue;
        this.listQueueByPriority = listQueueByPriority;
        this.getQueuePosition = getQueuePosition;
        this.callNextPatient = callNextPatient;
        this.reclassifyPriority = reclassifyPriority;
        this.cancelQueueEntry = cancelQueueEntry;
    }

    @PostMapping
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Registra paciente na fila",
            description = "Adiciona um paciente a uma fila específica com uma cor de risco.")
    @ApiResponse(responseCode = "201", description = "Paciente registrado com sucesso")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Perfil sem permissão (requer ROLE_MEDICO)")
    @ApiResponse(responseCode = "422", description = "Regra de negócio violada")
    public ResponseEntity<QueueEntryResponseDTO> registerQueue(@RequestBody @Valid RegisterQueueRequestDTO request) {
        QueueEntry entry = registerPatientInQueue.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(QueueEntryResponseDTO.fromEntity(entry));
    }

    @GetMapping
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Lista fila por prioridade",
            description = "Retorna a fila de um procedimento ordenada pelos critérios do SUS.")
    @ApiResponse(responseCode = "200", description = "Lista recuperada com sucesso")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Perfil sem permissão (requer ROLE_MEDICO)")
    public ResponseEntity<List<QueueEntryResponseDTO>> listQueue(
            @RequestParam UUID procedureId,
            @RequestParam(required = false) EQueueStatus status,
            @RequestParam(required = false) ERiskColor riskColor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ListQueueByPriorityDTO dto = new ListQueueByPriorityDTO(procedureId, status, riskColor, page, size);
        List<QueueEntry> queue = listQueueByPriority.run(dto);
        return ResponseEntity.ok(queue.stream().map(QueueEntryResponseDTO::fromEntity).toList());
    }

    @GetMapping("/{id}/position")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    @Operation(
            summary = "Busca posição do paciente",
            description = "Retorna a posição atual e os dados da entrada na fila.")
    @ApiResponse(responseCode = "200", description = "Posição encontrada")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Perfil sem permissão")
    @ApiResponse(responseCode = "404", description = "Entrada não encontrada")
    public ResponseEntity<QueuePositionResponseDTO> getQueuePosition(@PathVariable UUID id) {
        QueueEntryRiskQueuePosition result = getQueuePosition.run(id);
        return ResponseEntity.ok(new QueuePositionResponseDTO(
                result.posicaoCalculada(),
                QueueEntryResponseDTO.fromEntity(result.queueEntry())));
    }

    @PostMapping("/call-next")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Chama próximo paciente",
            description = "Retira o próximo paciente da fila (maior prioridade) para atendimento.")
    @ApiResponse(responseCode = "200", description = "Paciente chamado")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Perfil sem permissão (requer ROLE_MEDICO)")
    @ApiResponse(responseCode = "404", description = "Fila vazia")
    public ResponseEntity<QueueEntryResponseDTO> callNext() {
        QueueEntry entry = callNextPatient.run();
        return ResponseEntity.ok(QueueEntryResponseDTO.fromEntity(entry));
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Reclassifica risco",
            description = "Altera a cor de risco de uma entrada na fila, afetando sua prioridade.")
    @ApiResponse(responseCode = "200", description = "Prioridade atualizada")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Perfil sem permissão (requer ROLE_MEDICO)")
    @ApiResponse(responseCode = "404", description = "Entrada não encontrada")
    @ApiResponse(responseCode = "422", description = "Status atual não permite reclassificação")
    public ResponseEntity<QueueEntryResponseDTO> reclassifyPriority(
            @PathVariable UUID id,
            @RequestBody @Valid ReclassifyPriorityRequestDTO request) {
        QueueUpdateRiskColorDTO updateRiskColorDTO = new QueueUpdateRiskColorDTO(id, request.riskColor());
        QueueEntry entry = reclassifyPriority.run(updateRiskColorDTO).queueEntry();
        return ResponseEntity.ok(QueueEntryResponseDTO.fromEntity(entry));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Cancela entrada na fila",
            description = "Cancela um paciente da fila mediante justificativa.")
    @ApiResponse(responseCode = "204", description = "Cancelamento realizado")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Perfil sem permissão (requer ROLE_MEDICO)")
    @ApiResponse(responseCode = "404", description = "Entrada não encontrada")
    @ApiResponse(responseCode = "422", description = "Status não permite cancelamento")
    public ResponseEntity<Void> cancelQueueEntry(
            @PathVariable UUID id,
            @RequestBody @Valid CancelQueueRequestDTO request) {
        QueueCancelDTO queueCancelDTO = new QueueCancelDTO(id, request.motivoCancelamento());
        cancelQueueEntry.run(queueCancelDTO);
        return ResponseEntity.noContent().build();
    }
}
