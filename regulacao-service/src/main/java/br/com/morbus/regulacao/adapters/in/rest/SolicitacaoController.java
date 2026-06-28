package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoCreatedResponseDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoRequestDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoStatusResponseDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoSummaryDTO;
import br.com.morbus.regulacao.adapters.security.UserPrincipal;
import br.com.morbus.regulacao.domain.dto.ListarSolicitacoesQuery;
import br.com.morbus.regulacao.domain.dto.UsuarioContexto;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.IConsultarStatusSolicitacao;
import br.com.morbus.regulacao.ports.in.ICriarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.ICancelarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.IListarSolicitacoesUseCase;
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
public class SolicitacaoController {

    private final ICriarSolicitacaoUseCase criarSolicitacaoUseCase;
    private final IListarSolicitacoesUseCase listarSolicitacoesUseCase;
    private final ICancelarSolicitacaoUseCase deletarSolicitacaoUseCase;
    private final IConsultarStatusSolicitacao statusSolicitacao;

    public SolicitacaoController(ICriarSolicitacaoUseCase criarSolicitacaoUseCase,
                                 IListarSolicitacoesUseCase listarSolicitacoesUseCase,
                                 ICancelarSolicitacaoUseCase deletarSolicitacaoUseCase,
                                 IConsultarStatusSolicitacao statusSolicitacao) {
        this.criarSolicitacaoUseCase = criarSolicitacaoUseCase;
        this.listarSolicitacoesUseCase = listarSolicitacoesUseCase;
        this.deletarSolicitacaoUseCase = deletarSolicitacaoUseCase;
        this.statusSolicitacao = statusSolicitacao;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'MEDICO')")
    public ResponseEntity<SolicitacaoCreatedResponseDTO> criar(
            @Valid @RequestBody SolicitacaoRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Solicitacao solicitacao = criarSolicitacaoUseCase.execute(request.toCommand(principal.userId()));

        URI location = URI.create("/api/v1/solicitacoes/" + solicitacao.getId());
        return ResponseEntity.created(location)
                .body(SolicitacaoCreatedResponseDTO.fromDomain(solicitacao));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'SOLICITANTE')")
    public ResponseEntity<Page<SolicitacaoSummaryDTO>> listar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) EStatusSolicitacao status,
            @RequestParam(required = false) UUID procedureId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        UUID unidadeFiltro = "ROLE_SOLICITANTE".equals(principal.role())
                ? principal.unitId()
                : unidadeId;

        var query = new ListarSolicitacoesQuery(unidadeFiltro, status, procedureId, page, size);
        Page<SolicitacaoSummaryDTO> result = listarSolicitacoesUseCase.execute(query)
                .map(SolicitacaoSummaryDTO::fromDomain);

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'SOLICITANTE')")
    public ResponseEntity<?> cancelarSolicitacao(@PathVariable UUID id) {
        deletarSolicitacaoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'SOLICITANTE')")
    public ResponseEntity<SolicitacaoStatusResponseDTO> consultarStatusSolicitacao (@PathVariable UUID id,
                                                                                    @AuthenticationPrincipal UserPrincipal principal) {
        SolicitacaoStatusResponseDTO solicitacao = SolicitacaoStatusResponseDTO
                .fromDomain(statusSolicitacao.execute(id, UsuarioContexto.userPrincipalToContexto(principal)));
        return ResponseEntity.ok(solicitacao);
    }
}
