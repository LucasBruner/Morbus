package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoCreatedResponseDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoRequestDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoSummaryDTO;
import br.com.morbus.regulacao.adapters.out.security.UserPrincipal;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.ICriarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.IListarSolicitacoesUseCase;
import br.com.morbus.regulacao.ports.in.dto.ListarSolicitacoesQuery;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/solicitacoes")
public class SolicitacaoController {

    private final ICriarSolicitacaoUseCase criarSolicitacaoUseCase;
    private final IListarSolicitacoesUseCase listarSolicitacoesUseCase;

    public SolicitacaoController(ICriarSolicitacaoUseCase criarSolicitacaoUseCase,
                                 IListarSolicitacoesUseCase listarSolicitacoesUseCase) {
        this.criarSolicitacaoUseCase = criarSolicitacaoUseCase;
        this.listarSolicitacoesUseCase = listarSolicitacoesUseCase;
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID unidadeFiltro = "ROLE_SOLICITANTE".equals(principal.role())
                ? principal.unitId()
                : unidadeId;

        var query = new ListarSolicitacoesQuery(unidadeFiltro, status, procedureId, page, size);
        Page<SolicitacaoSummaryDTO> result = listarSolicitacoesUseCase.execute(query)
                .map(SolicitacaoSummaryDTO::fromDomain);

        return ResponseEntity.ok(result);
    }
}
