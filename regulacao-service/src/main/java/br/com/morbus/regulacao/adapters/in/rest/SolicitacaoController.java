package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoCreatedResponseDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.SolicitacaoRequestDTO;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.ICriarSolicitacaoUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/solicitacoes")
public class SolicitacaoController {

    private final ICriarSolicitacaoUseCase criarSolicitacaoUseCase;

    public SolicitacaoController(ICriarSolicitacaoUseCase criarSolicitacaoUseCase) {
        this.criarSolicitacaoUseCase = criarSolicitacaoUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'MEDICO')")
    public ResponseEntity<SolicitacaoCreatedResponseDTO> criar(
            @Valid @RequestBody SolicitacaoRequestDTO request,
            @AuthenticationPrincipal String solicitadoPor) {

        Solicitacao solicitacao = criarSolicitacaoUseCase.execute(request.toCommand(solicitadoPor));

        URI location = URI.create("/api/v1/solicitacoes/" + solicitacao.getId());
        return ResponseEntity.created(location)
                .body(SolicitacaoCreatedResponseDTO.fromDomain(solicitacao));
    }
}
