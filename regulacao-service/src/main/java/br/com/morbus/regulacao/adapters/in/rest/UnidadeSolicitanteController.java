package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.adapters.in.rest.dto.UnidadeSolicitanteRequestDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.UnidadeSolicitanteResponseDTO;
import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;
import br.com.morbus.regulacao.ports.in.IBuscarUnidadeSolicitanteUseCase;
import br.com.morbus.regulacao.ports.in.ICadastrarUnidadeSolicitanteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Unidades Solicitantes", description = "Cadastro e consulta de unidades de saúde solicitantes de regulação")
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
    @Operation(
            summary = "Cadastra unidade solicitante",
            description = "Cria uma nova unidade de saúde apta a solicitar regulação de procedimentos.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UnidadeSolicitanteRequestDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "cnes": "2077469",
                                      "nome": "UBS Jardim das Flores",
                                      "endereco": "Rua das Acácias, 123 - São Paulo/SP",
                                      "telefone": "(11) 4002-8922"
                                    }""")
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Unidade cadastrada com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UnidadeSolicitanteResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "cnes": "2077469",
                                      "nome": "UBS Jardim das Flores",
                                      "endereco": "Rua das Acácias, 123 - São Paulo/SP",
                                      "telefone": "(11) 4002-8922"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (requer ROLE_REGULADOR)", content = @Content),
            @ApiResponse(responseCode = "409", description = "Já existe unidade cadastrada com o mesmo CNES", content = @Content)
    })
    public ResponseEntity<UnidadeSolicitanteResponseDTO> cadastrar(@RequestBody @Valid UnidadeSolicitanteRequestDTO request) {
        UnidadeSolicitante unidade = cadastrarUnidadeSolicitanteUseCase.execute(request.toCommand());
        URI location = URI.create("/api/v1/unidades-solicitantes/" + unidade.getId());
        return ResponseEntity.created(location).body(UnidadeSolicitanteResponseDTO.fromDomain(unidade));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('REGULADOR', 'SOLICITANTE')")
    @Operation(
            summary = "Busca unidade solicitante por ID",
            description = "Retorna os dados cadastrais de uma unidade solicitante.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unidade encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UnidadeSolicitanteResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão", content = @Content),
            @ApiResponse(responseCode = "404", description = "Unidade não encontrada", content = @Content)
    })
    public ResponseEntity<UnidadeSolicitanteResponseDTO> buscarPorId(@PathVariable UUID id) {
        UnidadeSolicitante unidade = buscarUnidadeSolicitanteUseCase.execute(id);
        return ResponseEntity.ok(UnidadeSolicitanteResponseDTO.fromDomain(unidade));
    }
}