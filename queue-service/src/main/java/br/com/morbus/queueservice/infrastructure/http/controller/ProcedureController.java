package br.com.morbus.queueservice.infrastructure.http.controller;

import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.repository.ProcedurePageResult;
import br.com.morbus.queueservice.domain.usecase.GetProcedureByCodigo;
import br.com.morbus.queueservice.domain.usecase.GetProcedureById;
import br.com.morbus.queueservice.domain.usecase.ListProcedures;
import br.com.morbus.queueservice.domain.usecase.dto.PageResponseDTO;
import br.com.morbus.queueservice.domain.usecase.dto.ProcedureResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/procedures")
@Tag(name = "Procedures", description = "Endpoints para gerenciamento de procedimentos")
@PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
public class ProcedureController {

    private final ListProcedures listProcedures;
    private final GetProcedureById getProcedureById;
    private final GetProcedureByCodigo getProcedureByCodigo;

    public ProcedureController(ListProcedures listProcedures,
                               GetProcedureById getProcedureById,
                               GetProcedureByCodigo getProcedureByCodigo) {
        this.listProcedures = listProcedures;
        this.getProcedureById = getProcedureById;
        this.getProcedureByCodigo = getProcedureByCodigo;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    @Operation(
            summary = "Lista todos os procedimentos",
            description = "Retorna uma lista paginada de procedimentos do catálogo SIGTAP disponível no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista recuperada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcedureResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
    })
    public ResponseEntity<PageResponseDTO<ProcedureResponseDTO>> listAllProcedures(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        ProcedurePageResult result = listProcedures.run(page, size);
        List<ProcedureResponseDTO> content = result.content().stream()
                .map(ProcedureResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(PageResponseDTO.of(content, page, size, result.totalElements()));
    }

    @GetMapping(params = "codigo")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    @Operation(
            summary = "Busca procedimento por código SIGTAP",
            description = "Busca um procedimento específico utilizando o seu código SIGTAP único.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Procedimento encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcedureResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Procedimento não encontrado", content = @Content)
    })
    public ResponseEntity<ProcedureResponseDTO> getByCodigo(@RequestParam String codigo) {
        Procedure procedure = getProcedureByCodigo.run(codigo);
        return ResponseEntity.ok(ProcedureResponseDTO.fromEntity(procedure));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    @Operation(
            summary = "Busca procedimento por ID",
            description = "Retorna os detalhes de um procedimento através do seu UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Procedimento encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcedureResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Procedimento não encontrado", content = @Content)
    })
    public ResponseEntity<ProcedureResponseDTO> getById(@PathVariable UUID id) {
        Procedure procedure = getProcedureById.run(id);
        return ResponseEntity.ok(ProcedureResponseDTO.fromEntity(procedure));
    }
}
