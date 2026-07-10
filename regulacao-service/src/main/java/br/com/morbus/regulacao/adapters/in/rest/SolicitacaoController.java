package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.adapters.in.rest.dto.ComplementarSolicitacaoRequestDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoCreatedResponseDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoRequestDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoStatusResponseDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoSummaryDTO;
import br.com.morbus.regulacao.adapters.security.UserPrincipal;
import br.com.morbus.regulacao.domain.dto.ListarSolicitacoesQuery;
import br.com.morbus.regulacao.domain.dto.UsuarioContexto;
import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.ICancelarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.IComplementarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.IConsultarStatusSolicitacao;
import br.com.morbus.regulacao.ports.in.ICriarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.IListarSolicitacoesUseCase;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/solicitacoes")
@Tag(name = "Solicitações", description = "Criação, consulta e cancelamento de solicitações de regulação")
public class SolicitacaoController {

    private final ICriarSolicitacaoUseCase criarSolicitacaoUseCase;
    private final IListarSolicitacoesUseCase listarSolicitacoesUseCase;
    private final ICancelarSolicitacaoUseCase deletarSolicitacaoUseCase;
    private final IConsultarStatusSolicitacao statusSolicitacao;
    private final IComplementarSolicitacaoUseCase complementarSolicitacaoUseCase;

    public SolicitacaoController(ICriarSolicitacaoUseCase criarSolicitacaoUseCase,
                                 IListarSolicitacoesUseCase listarSolicitacoesUseCase,
                                 ICancelarSolicitacaoUseCase deletarSolicitacaoUseCase,
                                 IConsultarStatusSolicitacao statusSolicitacao,
                                 IComplementarSolicitacaoUseCase complementarSolicitacaoUseCase) {
        this.criarSolicitacaoUseCase = criarSolicitacaoUseCase;
        this.listarSolicitacoesUseCase = listarSolicitacoesUseCase;
        this.deletarSolicitacaoUseCase = deletarSolicitacaoUseCase;
        this.statusSolicitacao = statusSolicitacao;
        this.complementarSolicitacaoUseCase = complementarSolicitacaoUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('SOLICITANTE')")
    @Operation(
            summary = "Cria solicitação de regulação",
            description = "Registra uma nova solicitação de regulação de procedimento para um paciente, direcionada a uma fila de espera ou fila regulada.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SolicitacaoRequestDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "patientId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "procedureId": "7b3c1a2d-9e4f-4a8b-b6d1-1f2e3a4b5c6d",
                                      "cid": "I10",
                                      "justificativaClinica": "Paciente hipertenso, necessita avaliação cardiológica com urgência",
                                      "profissionalSolicitante": "Dra. Ana Souza",
                                      "crmProfissional": "123456-SP",
                                      "unitSolicitanteId": "9c7b1a2d-3e4f-4a8b-b6d1-1f2e3a4b5c6d",
                                      "destino": "FILA_REGULADA"
                                    }""")
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Solicitação criada com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SolicitacaoCreatedResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": "a1b2c3d4-e5f6-7890-ab12-cd34ef567890",
                                      "patientId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "procedureId": "7b3c1a2d-9e4f-4a8b-b6d1-1f2e3a4b5c6d",
                                      "cid": "I10",
                                      "destino": "FILA_REGULADA",
                                      "riskColor": null,
                                      "status": "AGUARDANDO",
                                      "criadaEm": "2026-07-06T10:30:00"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (requer ROLE_SOLICITANTE)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Unidade solicitante não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Já existe solicitação equivalente em aberto para o paciente/procedimento", content = @Content),
            @ApiResponse(responseCode = "422", description = "Cota do procedimento esgotada para a unidade/período", content = @Content)
    })
    public ResponseEntity<SolicitacaoCreatedResponseDTO> criar(
            @Valid @RequestBody SolicitacaoRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Solicitacao solicitacao = criarSolicitacaoUseCase.execute(request.toCommand(principal.userId()));

        URI location = URI.create("/api/v1/solicitacoes/" + solicitacao.getId());
        return ResponseEntity.created(location)
                .body(SolicitacaoCreatedResponseDTO.fromDomain(solicitacao));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'REGULADOR')")
    @Operation(
            summary = "Lista solicitações",
            description = "Retorna as solicitações de regulação, filtráveis por unidade, status e procedimento. Usuários com ROLE_SOLICITANTE só visualizam solicitações da própria unidade.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista recuperada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SolicitacaoSummaryDTO.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão", content = @Content)
    })
    public ResponseEntity<Page<SolicitacaoSummaryDTO>> listar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) EStatusSolicitacao status,
            @RequestParam(required = false) EDestino destino,
            @RequestParam(required = false) UUID procedureId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        UUID unidadeFiltro = "ROLE_SOLICITANTE".equals(principal.role())
                ? principal.unitId()
                : unidadeId;

        var query = new ListarSolicitacoesQuery(unidadeFiltro, status, destino, procedureId, page, size);
        Page<SolicitacaoSummaryDTO> result = listarSolicitacoesUseCase.execute(query)
                .map(SolicitacaoSummaryDTO::fromDomain);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'SOLICITANTE')")
    @Operation(
            summary = "Cancela solicitação",
            description = "Cancela uma solicitação de regulação. Permitido apenas enquanto o status for AGUARDANDO.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cancelamento realizado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (requer ROLE_MEDICO ou ROLE_SOLICITANTE)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "Status atual não permite cancelamento (diferente de AGUARDANDO)", content = @Content)
    })
    public ResponseEntity<?> cancelarSolicitacao(@PathVariable UUID id) {
        deletarSolicitacaoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'REGULADOR')")
    @Operation(
            summary = "Consulta status da solicitação",
            description = "Retorna os dados completos e o status atual de uma solicitação de regulação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SolicitacaoStatusResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão", content = @Content),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada", content = @Content)
    })
    public ResponseEntity<SolicitacaoStatusResponseDTO> consultarStatusSolicitacao(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        SolicitacaoStatusResponseDTO solicitacao = SolicitacaoStatusResponseDTO
                .fromDomain(statusSolicitacao.execute(id, new UsuarioContexto(principal.role(), principal.userId())));
        return ResponseEntity.ok(solicitacao);
    }

    @PostMapping("/{id}/complementar")
    @PreAuthorize("hasRole('SOLICITANTE')")
    @Operation(
            summary = "Complementa solicitação devolvida",
            description = "Complementa uma solicitação devolvida pelo regulador com informações faltantes. Apenas campos enviados (não nulos) são atualizados. Após a complementação o status volta para AGUARDANDO.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ComplementarSolicitacaoRequestDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "cid": "I11.9",
                                      "justificativaClinica": "Complemento: paciente com HAS estagio 3, sem resposta a 3 anti-hipertensivos.",
                                      "crmProfissional": "CRM/SP 12345"
                                    }""")
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação complementada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SolicitacaoStatusResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (requer ROLE_SOLICITANTE)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Solicitação não encontrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "Status da solicitação não permite complementação — apenas DEVOLVIDA", content = @Content)
    })
    public ResponseEntity<SolicitacaoStatusResponseDTO> complementar(
            @PathVariable UUID id,
            @Valid @RequestBody ComplementarSolicitacaoRequestDTO request) {
        Solicitacao solicitacao = complementarSolicitacaoUseCase.execute(request.toCommand(id));
        return ResponseEntity.ok(SolicitacaoStatusResponseDTO.fromDomain(solicitacao));
    }
}
