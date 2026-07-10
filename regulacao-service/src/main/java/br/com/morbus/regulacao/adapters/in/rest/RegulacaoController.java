package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.adapters.in.rest.dto.AvaliarSolicitacaoRequestDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.AvaliarSolicitacaoResponseDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.ReclassificarRiscoRequestDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoStatusResponseDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoSummaryDTO;
import br.com.morbus.regulacao.adapters.security.UserPrincipal;
import br.com.morbus.regulacao.domain.dto.ESortDirection;
import br.com.morbus.regulacao.domain.dto.ListarSolicitacoesQuery;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.IAvaliarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.IListarSolicitacoesUseCase;
import br.com.morbus.regulacao.ports.in.IReclassificarRiscoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/regulacao")
@Tag(name = "Regulação", description = "Fila de regulação, avaliação de solicitações e reclassificação de risco")
public class RegulacaoController {

    private final IListarSolicitacoesUseCase listarSolicitacoesUseCase;
    private final IAvaliarSolicitacaoUseCase avaliarSolicitacaoUseCase;
    private final IReclassificarRiscoUseCase reclassificarRiscoUseCase;

    public RegulacaoController(IListarSolicitacoesUseCase listarSolicitacoesUseCase,
                               IAvaliarSolicitacaoUseCase avaliarSolicitacaoUseCase,
                               IReclassificarRiscoUseCase reclassificarRiscoUseCase) {
        this.listarSolicitacoesUseCase = listarSolicitacoesUseCase;
        this.avaliarSolicitacaoUseCase = avaliarSolicitacaoUseCase;
        this.reclassificarRiscoUseCase = reclassificarRiscoUseCase;
    }

    @GetMapping("/pendentes")
    @PreAuthorize("hasRole('REGULADOR')")
    @Operation(
            summary = "Lista solicitações pendentes",
            description = "Retorna as solicitações com status AGUARDANDO, aguardando primeira avaliação do regulador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista recuperada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SolicitacaoSummaryDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (requer ROLE_REGULADOR)", content = @Content)
    })
    public ResponseEntity<Page<SolicitacaoSummaryDTO>> listarPendentes(
            @RequestParam(required = false) UUID procedureId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(listarPorStatus(EStatusSolicitacao.AGUARDANDO, procedureId, page, size));
    }

    @GetMapping("/pendentes-vaga")
    @PreAuthorize("hasRole('REGULADOR')")
    @Operation(
            summary = "Lista solicitações pendentes de vaga",
            description = "Retorna as solicitações com status PENDENTE, aprovadas mas aguardando abertura de cota para a unidade/procedimento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista recuperada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SolicitacaoSummaryDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (requer ROLE_REGULADOR)", content = @Content)
    })
    public ResponseEntity<Page<SolicitacaoSummaryDTO>> listarPendentesVaga(
            @RequestParam(required = false) UUID procedureId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(listarPorStatus(EStatusSolicitacao.PENDENTE, procedureId, page, size));
    }

    @PostMapping("/{id}/avaliar")
    @PreAuthorize("hasRole('REGULADOR')")
    @Operation(
            summary = "Avalia solicitação",
            description = "Registra o parecer do regulador sobre uma solicitação: autorizar, encaminhar para fila de espera, negar, devolver ou marcar como pendente de cota. `riskColorDefinido` é obrigatório para AUTORIZAR/FILA_ESPERA; `justificativa` é obrigatória para NEGAR.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AvaliarSolicitacaoRequestDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "decisao": "AUTORIZAR",
                                      "riskColorDefinido": "AMARELO",
                                      "justificativa": null,
                                      "unidadeExecutanteId": "9c7b1a2d-3e4f-4a8b-b6d1-1f2e3a4b5c6d"
                                    }""")
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avaliação registrada com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AvaliarSolicitacaoResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "parecerId": "5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b",
                                      "solicitacaoId": "a1b2c3d4-e5f6-7890-ab12-cd34ef567890",
                                      "reguladorId": "1a2b3c4d-5e6f-7890-ab12-cd34ef567890",
                                      "decisao": "AUTORIZAR",
                                      "justificativa": null,
                                      "emitidoEm": "2026-07-06T11:00:00",
                                      "novoStatus": "APROVADA"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos ou campo obrigatório ausente para a decisão informada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (requer ROLE_REGULADOR)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "Status atual da solicitação não permite a decisão informada", content = @Content)
    })
    @Transactional
    public ResponseEntity<AvaliarSolicitacaoResponseDTO> avaliar(
            @PathVariable UUID id,
            @Valid @RequestBody AvaliarSolicitacaoRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var result = avaliarSolicitacaoUseCase.execute(request.toCommand(id, principal.userId()));
        return ResponseEntity.ok(AvaliarSolicitacaoResponseDTO.fromResult(result));
    }

    @PatchMapping("/solicitacoes/{id}/risco")
    @PreAuthorize("hasRole('REGULADOR')")
    @Operation(
            summary = "Reclassifica risco da solicitação",
            description = "Altera a classificação de risco de uma solicitação ainda não avaliada.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ReclassificarRiscoRequestDTO.class),
                            examples = @ExampleObject(value = """
                                    { "riskColor": "VERMELHO" }""")
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Risco reclassificado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SolicitacaoStatusResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (requer ROLE_REGULADOR)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "Status atual da solicitação não permite reclassificação", content = @Content)
    })
    @Transactional
    public ResponseEntity<SolicitacaoStatusResponseDTO> reclassificarRisco(
            @PathVariable UUID id,
            @Valid @RequestBody ReclassificarRiscoRequestDTO request) {
        Solicitacao solicitacao = reclassificarRiscoUseCase.execute(request.toCommand(id));
        return ResponseEntity.ok(SolicitacaoStatusResponseDTO.fromDomain(solicitacao));
    }

    private Page<SolicitacaoSummaryDTO> listarPorStatus(EStatusSolicitacao status, UUID procedureId, int page, int size) {
        var query = new ListarSolicitacoesQuery(null, status, procedureId, page, size, ESortDirection.ASC);
        return PageMapper.toSpringPage(listarSolicitacoesUseCase.execute(query).map(SolicitacaoSummaryDTO::fromDomain));
    }
}
