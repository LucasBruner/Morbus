package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.adapters.in.rest.dto.AvaliarSolicitacaoRequestDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.AvaliarSolicitacaoResponseDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.ReclassificarRiscoRequestDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoStatusResponseDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoSummaryDTO;
import br.com.morbus.regulacao.adapters.security.UserPrincipal;
import br.com.morbus.regulacao.domain.dto.ListarSolicitacoesQuery;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.IAvaliarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.IListarSolicitacoesUseCase;
import br.com.morbus.regulacao.ports.in.IReclassificarRiscoUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/regulacao")
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
    public ResponseEntity<Page<SolicitacaoSummaryDTO>> listarPendentes(
            @RequestParam(required = false) UUID procedureId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(listarPorStatus(EStatusSolicitacao.AGUARDANDO, procedureId, page, size));
    }

    @GetMapping("/pendentes-vaga")
    @PreAuthorize("hasRole('REGULADOR')")
    public ResponseEntity<Page<SolicitacaoSummaryDTO>> listarPendentesVaga(
            @RequestParam(required = false) UUID procedureId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(listarPorStatus(EStatusSolicitacao.PENDENTE, procedureId, page, size));
    }

    @PostMapping("/{id}/avaliar")
    @PreAuthorize("hasRole('REGULADOR')")
    public ResponseEntity<AvaliarSolicitacaoResponseDTO> avaliar(
            @PathVariable UUID id,
            @Valid @RequestBody AvaliarSolicitacaoRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var result = avaliarSolicitacaoUseCase.execute(request.toCommand(id, principal.userId()));
        return ResponseEntity.ok(AvaliarSolicitacaoResponseDTO.fromResult(result));
    }

    @PatchMapping("/solicitacoes/{id}/risco")
    @PreAuthorize("hasRole('REGULADOR')")
    public ResponseEntity<SolicitacaoStatusResponseDTO> reclassificarRisco(
            @PathVariable UUID id,
            @Valid @RequestBody ReclassificarRiscoRequestDTO request) {
        Solicitacao solicitacao = reclassificarRiscoUseCase.execute(request.toCommand(id));
        return ResponseEntity.ok(SolicitacaoStatusResponseDTO.fromDomain(solicitacao));
    }

    private Page<SolicitacaoSummaryDTO> listarPorStatus(EStatusSolicitacao status, UUID procedureId, int page, int size) {
        var query = new ListarSolicitacoesQuery(null, status, procedureId, page, size, Sort.Direction.ASC);
        return listarSolicitacoesUseCase.execute(query).map(SolicitacaoSummaryDTO::fromDomain);
    }
}
