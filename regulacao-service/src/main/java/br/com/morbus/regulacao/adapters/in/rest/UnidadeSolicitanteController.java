package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.adapters.in.rest.dto.UnidadeSolicitanteRequestDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.UnidadeSolicitanteResponseDTO;
import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;
import br.com.morbus.regulacao.ports.in.IBuscarUnidadeSolicitanteUseCase;
import br.com.morbus.regulacao.ports.in.ICadastrarUnidadeSolicitanteUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/unidades-solicitantes")
public class UnidadeSolicitanteController {

    private final ICadastrarUnidadeSolicitanteUseCase cadastrarUnidadeSolicitanteUseCase;
    private final IBuscarUnidadeSolicitanteUseCase buscarUnidadeSolicitanteUseCase;

    public UnidadeSolicitanteController(ICadastrarUnidadeSolicitanteUseCase cadastrarUnidadeSolicitanteUseCase,
                                        IBuscarUnidadeSolicitanteUseCase buscarUnidadeSolicitanteUseCase) {
        this.cadastrarUnidadeSolicitanteUseCase = cadastrarUnidadeSolicitanteUseCase;
        this.buscarUnidadeSolicitanteUseCase = buscarUnidadeSolicitanteUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('REGULADOR')")
    public ResponseEntity<UnidadeSolicitanteResponseDTO> cadastrar(@RequestBody @Valid UnidadeSolicitanteRequestDTO request) {
        UnidadeSolicitante unidade = cadastrarUnidadeSolicitanteUseCase.execute(request.toCommand());
        URI location = URI.create("/api/v1/unidades-solicitantes/" + unidade.getId());
        return ResponseEntity.created(location).body(UnidadeSolicitanteResponseDTO.fromDomain(unidade));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('REGULADOR', 'SOLICITANTE')")
    public ResponseEntity<UnidadeSolicitanteResponseDTO> buscarPorId(@PathVariable UUID id) {
        UnidadeSolicitante unidade = buscarUnidadeSolicitanteUseCase.execute(id);
        return ResponseEntity.ok(UnidadeSolicitanteResponseDTO.fromDomain(unidade));
    }
}