package br.com.morbus.queueservice.domain.usecase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Dados para criar/atualizar a cota diária de FILA_ESPERA de uma unidade+procedimento")
public record QuotaRequestDTO(
        @NotNull
        @Schema(description = "ID da unidade de execução", example = "7b3c1a2d-9e4f-4a8b-b6d1-1f2e3a4b5c6d")
        UUID unitId,

        @NotNull
        @Schema(description = "ID do procedimento SIGTAP", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID procedureId,

        @NotNull
        @Min(1)
        @Schema(description = "Número máximo de entradas em FILA_ESPERA por dia para esta combinação", example = "10")
        Integer maxPerDay
) {
}
