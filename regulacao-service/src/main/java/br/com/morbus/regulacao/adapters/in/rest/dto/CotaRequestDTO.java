package br.com.morbus.regulacao.adapters.in.rest.dto;

import br.com.morbus.regulacao.ports.in.dto.GerenciarCotaCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CotaRequestDTO(@NotNull(message = "unidId é obrigatório") UUID unitId,
                             @NotNull(message = "procedureId é obrigatório") UUID procedureId,
                             @Min(value = 1, message = "maxPerPeriodo deve ser >= 1") int maxPerPeriod,
                             @NotNull(message = "periodStart é obrigatório") LocalDate periodStart) {
    public static GerenciarCotaCommand toCommand(CotaRequestDTO requestDTO) {
        return new GerenciarCotaCommand(requestDTO.unitId,
                requestDTO.procedureId,
                requestDTO.maxPerPeriod,
                requestDTO.periodStart);
    }
}
