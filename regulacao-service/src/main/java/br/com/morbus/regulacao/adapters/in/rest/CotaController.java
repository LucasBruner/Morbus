package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.adapters.in.rest.dto.CotaRequestDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.CotaResponseDTO;
import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.ports.in.IConsultarCotasUseCase;
import br.com.morbus.regulacao.ports.in.IGerenciarCotaUseCase;
import br.com.morbus.regulacao.ports.in.dto.ConsultarCotasQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/regulacao/cotas")
@Tag(name = "Cotas", description = "Gerenciamento e consulta de cotas de procedimentos por unidade/período")
public class CotaController {
    private final IConsultarCotasUseCase consultarCotasUseCase;
    private final IGerenciarCotaUseCase gerenciarCotaUseCase;

    public CotaController(IConsultarCotasUseCase consultarCotasUseCase,
                          IGerenciarCotaUseCase gerenciarCotaUseCase) {
        this.consultarCotasUseCase = consultarCotasUseCase;
        this.gerenciarCotaUseCase = gerenciarCotaUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('REGULADOR')")
    @Operation(
            summary = "Cria ou atualiza cota",
            description = "Define a quantidade máxima de vagas de um procedimento para uma unidade em um período (mês). Se já existir cota para a combinação unitId/procedureId/periodStart, ela é atualizada.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CotaRequestDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "unitId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "procedureId": "7b3c1a2d-9e4f-4a8b-b6d1-1f2e3a4b5c6d",
                                      "maxPerPeriod": 50,
                                      "periodStart": "2026-08-01"
                                    }""")
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cota criada ou atualizada com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CotaResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": "a1b2c3d4-e5f6-7890-ab12-cd34ef567890",
                                      "unitId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "procedureId": "7b3c1a2d-9e4f-4a8b-b6d1-1f2e3a4b5c6d",
                                      "maxPerPeriod": 50,
                                      "currentCount": 0,
                                      "periodStart": "2026-08-01"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (requer ROLE_REGULADOR)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Unidade solicitante não encontrada", content = @Content)
    })
    public ResponseEntity<CotaResponseDTO> upsert(@RequestBody @Valid CotaRequestDTO cotaRequestDTO) {
        Quota cota = gerenciarCotaUseCase.execute(CotaRequestDTO.toCommand(cotaRequestDTO));
        return ResponseEntity.ok(CotaResponseDTO.fromResult(cota));
    }

    @GetMapping
    @PreAuthorize("hasRole('REGULADOR')")
    @Operation(
            summary = "Lista cotas",
            description = "Retorna as cotas cadastradas, filtráveis por unidade, procedimento e mês de referência (formato yyyy-MM).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista recuperada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CotaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Parâmetro 'mes' fora do formato yyyy-MM", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão (requer ROLE_REGULADOR)", content = @Content)
    })
    public ResponseEntity<Page<CotaResponseDTO>> listar(@RequestParam (required = false) UUID unitId,
                                                        @RequestParam (required = false) UUID procedureId,
                                                        @Parameter(description = "Mês de referência no formato yyyy-MM", example = "2026-08")
                                                        @RequestParam (required = false) String mes,
                                                        @RequestParam (defaultValue = "0") @Min(0) int page,
                                                        @RequestParam (defaultValue = "20") @Min(1) @Max(100) int size) {
        LocalDate periodStart = mes != null ? YearMonth.parse(mes).atDay(1) : null;
        var query = new ConsultarCotasQuery(unitId, procedureId, periodStart, page, size);
        return ResponseEntity.ok(consultarCotasUseCase.execute(query).map(CotaResponseDTO::fromResult));
    }
}
