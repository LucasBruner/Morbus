package br.com.morbus.queueservice.infrastructure.http.controller;

import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.usecase.GetProcedureByCodigo;
import br.com.morbus.queueservice.domain.usecase.GetProcedureById;
import br.com.morbus.queueservice.domain.usecase.ListProcedures;
import br.com.morbus.queueservice.domain.usecase.dto.ProcedureResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
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
    @Operation(
            summary = "Lista todos os procedimentos",
            description = "Retorna uma lista paginada de procedimentos do catálogo SIGTAP disponível no sistema.",
            responses = {
                    @ApiResponse(description = "Ok", responseCode = "200")})
    public ResponseEntity<List<ProcedureResponseDTO>> listAllProcedures(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        List<Procedure> procedures = listProcedures.run(page, size);
        return ResponseEntity.ok(procedures.stream().map(ProcedureResponseDTO::fromEntity).toList());
    }

    @GetMapping(params = "codigo")
    @Operation(
            summary = "Busca procedimento por código SIGTAP",
            description = "Busca um procedimento específico utilizando o seu código SIGTAP único.",
            responses = {
                    @ApiResponse(description = "Ok", responseCode = "200"),
                    @ApiResponse(description = "Não encontrado", responseCode = "404")})
    public ResponseEntity<ProcedureResponseDTO> getByCodigo(@RequestParam String codigo) {
        Procedure procedure = getProcedureByCodigo.run(codigo);
        return ResponseEntity.ok(ProcedureResponseDTO.fromEntity(procedure));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca procedimento por ID",
            description = "Retorna os detalhes de um procedimento através do seu UUID.",
            responses = {
                    @ApiResponse(description = "Ok", responseCode = "200"),
                    @ApiResponse(description = "Não encontrado", responseCode = "404")})
    public ResponseEntity<ProcedureResponseDTO> getById(@PathVariable UUID id) {
        Procedure procedure = getProcedureById.run(id);
        return ResponseEntity.ok(ProcedureResponseDTO.fromEntity(procedure));
    }
}
