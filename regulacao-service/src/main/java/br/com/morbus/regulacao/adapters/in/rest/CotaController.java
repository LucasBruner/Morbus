package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.adapters.in.rest.dto.CotaRequestDTO;
import br.com.morbus.regulacao.adapters.in.rest.dto.CotaResponseDTO;
import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.ports.in.IConsultarCotasUseCase;
import br.com.morbus.regulacao.ports.in.IGerenciarCotaUseCase;
import br.com.morbus.regulacao.ports.in.dto.ConsultarCotasQuery;
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
    public ResponseEntity<CotaResponseDTO> upsert(@RequestBody @Valid CotaRequestDTO cotaRequestDTO) {
        Quota cota = gerenciarCotaUseCase.execute(CotaRequestDTO.toCommand(cotaRequestDTO));
        return ResponseEntity.ok(CotaResponseDTO.fromResult(cota));
    }

    @GetMapping
    @PreAuthorize("hasRole('REGULADOR')")
    public ResponseEntity<Page<CotaResponseDTO>> listar(@RequestParam (required = false) UUID unitId,
                                                        @RequestParam (required = false) UUID procedureId,
                                                        @RequestParam (required = false) String mes,
                                                        @RequestParam (defaultValue = "0") @Min(0) int page,
                                                        @RequestParam (defaultValue = "20") @Min(1) @Max(100) int size) {
        LocalDate periodStart = mes != null ? YearMonth.parse(mes).atDay(1) : null;
        var query = new ConsultarCotasQuery(unitId, procedureId, periodStart, page, size);
        return ResponseEntity.ok(consultarCotasUseCase.execute(query).map(CotaResponseDTO::fromResult));
    }
}
