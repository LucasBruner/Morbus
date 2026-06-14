package br.com.morbus.queueservice.infrastructure.http.controller;

import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.usecase.dto.ProcedureResponseDTO;
import br.com.morbus.queueservice.infrastructure.database.persistence.ProcedureRepositoryImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/procedures")
@Tag(name = "Procedures", description = "Endpoints para gerenciamento de procedimentos")
public class ProcedureController {

    private final ProcedureRepositoryImpl repository;

    public ProcedureController(ProcedureRepositoryImpl repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(
            summary = "Lista todos os procedimentos",
            description = "Retorna uma lista de procedimentos do catálogo SIGTAP disponível no sistema.",
            responses = {
                    @ApiResponse(description = "Ok", responseCode = "200"),
                    @ApiResponse(description = "Not found", responseCode = "404")})
    public ResponseEntity<List<ProcedureResponseDTO>> listAllProcedures() {
        List<Procedure> procedures = repository.findAll();
        return ResponseEntity.ok(procedures.stream().map(ProcedureResponseDTO::fromEntity).toList());
    }

    @GetMapping(params = "codigo")
    @Operation(
            summary = "Busca procedimento por código SIGTAP",
            description = "Busca um procedimento específico utilizando o seu código SIGTAP único.",
            responses = {
                    @ApiResponse(description = "Ok", responseCode = "200"),
                    @ApiResponse(description = "Not found", responseCode = "404")})
    public ResponseEntity<ProcedureResponseDTO> getByCodigo(@RequestParam String codigo) {
        return repository.findByCoProcedimento(codigo)
                .map(ProcedureResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Procedimento com código SIGTAP " + codigo + " não encontrado"));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca procedimento por ID",
            description = "Retorna os detalhes de um procedimento através do seu UUID.",
            responses = {
                    @ApiResponse(description = "Ok", responseCode = "200"),
                    @ApiResponse(description = "Not found", responseCode = "404")})
    public ResponseEntity<ProcedureResponseDTO> getById(@PathVariable("id") UUID id) {
        return repository.findById(id)
                .map(ProcedureResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Procedimento com ID " + id + " não encontrado"));
    }
}
