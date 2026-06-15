package br.com.morbus.queueservice.infrastructure.http.controller;

import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import br.com.morbus.queueservice.domain.usecase.*;
import br.com.morbus.queueservice.domain.usecase.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public QueueController(RegisterPatientInQueue registerPatientInQueue, ListQueueByPriority listQueueByPriority, GetQueuePosition getQueuePosition, CallNextPatient callNextPatient, ReclassifyPriority reclassifyPriority, CancelQueueEntry cancelQueueEntry) {
        this.registerPatientInQueue = registerPatientInQueue;
        this.listQueueByPriority = listQueueByPriority;
        this.getQueuePosition = getQueuePosition;
        this.callNextPatient = callNextPatient;
        this.reclassifyPriority = reclassifyPriority;
        this.cancelQueueEntry = cancelQueueEntry;
    }

    @PostMapping
    @Operation(
            summary = "Registra paciente na fila",
            description = "Adiciona um paciente a uma fila específica com uma cor de risco.")
    @ApiResponse(responseCode = "201", description = "Paciente registrado com sucesso")
    @ApiResponse(responseCode = "422", description = "Regra de negócio violada")
    public ResponseEntity<QueueEntryResponseDTO> registerQueue(@RequestBody @Valid RegisterQueueRequestDTO request) {
        QueueEntry entry = registerPatientInQueue.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(QueueEntryResponseDTO.fromEntity(entry));
    }

    @GetMapping
    @Operation(
            summary = "Lista fila por prioridade",
            description = "Retorna a fila de um procedimento ordenada pelos critérios do SUS.")
    @ApiResponse(responseCode = "200", description = "Lista recuperada com sucesso")
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
    @Operation(
            summary = "Busca posição do paciente",
            description = "Retorna a posição atual e os dados da entrada na fila.")
    @ApiResponse(responseCode = "200", description = "Posição encontrada")
    @ApiResponse(responseCode = "404", description = "Entrada não encontrada")
    public ResponseEntity<QueuePositionResponseDTO> getQueuePosition(@PathVariable UUID id) {
        QueueEntryRiskQueuePosition result = getQueuePosition.run(id);
        return ResponseEntity.ok(new QueuePositionResponseDTO(result.posicaoCalculada(), QueueEntryResponseDTO.fromEntity(result.queueEntry())));
    }

    @PostMapping("/call-next")
    @Operation(
            summary = "Chama próximo paciente",
            description = "Retira o próximo paciente da fila (maior prioridade) para atendimento.")
    @ApiResponse(responseCode = "200", description = "Paciente chamado")
    @ApiResponse(responseCode = "404", description = "Fila vazia")
    public ResponseEntity<QueueEntryResponseDTO> callNext() {
        QueueEntry entry = callNextPatient.run();
        return ResponseEntity.ok(QueueEntryResponseDTO.fromEntity(entry));
    }

    @PatchMapping("/{id}/priority")
    @Operation(
            summary = "Reclassifica risco",
            description = "Altera a cor de risco de uma entrada na fila, afetando sua prioridade.")
    @ApiResponse(responseCode = "200", description = "Prioridade atualizada")
    @ApiResponse(responseCode = "422", description = "Status atual não permite reclassificação")
    public ResponseEntity<QueueEntryResponseDTO> reclassifyPriority(@PathVariable UUID id, @RequestBody @Valid ReclassifyPriorityRequestDTO request) {
        QueueUpdateRiskColorDTO updateRiskColorDTO = new QueueUpdateRiskColorDTO(id, request.riskColor());
        QueueEntry entry = reclassifyPriority.run(updateRiskColorDTO).queueEntry();
        return ResponseEntity.ok(QueueEntryResponseDTO.fromEntity(entry));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Cancela entrada na fila",
            description = "Remove/Cancela um paciente da fila mediante justificativa.")
    @ApiResponse(responseCode = "200", description = "Cancelamento realizado")
    public ResponseEntity<Void> cancelQueueEntry(@PathVariable UUID id, @RequestBody @Valid CancelQueueRequestDTO request) {
        QueueCancelDTO queueCancelDTO = new QueueCancelDTO(id, request.motivoCancelamento());
        cancelQueueEntry.run(queueCancelDTO);
        return ResponseEntity.noContent().build();
    }
}
