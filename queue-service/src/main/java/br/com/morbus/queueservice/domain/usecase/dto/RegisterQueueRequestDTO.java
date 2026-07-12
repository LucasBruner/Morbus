package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.enums.EDestino;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Dados para registrar um paciente na fila")
public record RegisterQueueRequestDTO(
        @NotNull
        @Schema(description = "ID do paciente", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID patientId,

        @NotNull
        @Schema(description = "ID do procedimento SIGTAP", example = "7b3c1a2d-9e4f-4a8b-b6d1-1f2e3a4b5c6d")
        UUID procedureId,

        @NotNull
        @Schema(description = "Cor de risco clínico. FILA_ESPERA só aceita AZUL; outra cor retorna 422.", example = "AMARELO")
        ERiskColor riskColor,

        @Schema(description = "Tipo de fila. FILA_REGULADA (padrão) ou FILA_ESPERA.", example = "FILA_REGULADA")
        EDestino tipoFila,

        @Schema(description = "ID da solicitação de origem (regulacao-service), quando aplicável")
        UUID solicitacaoId,

        @Schema(description = "ID da unidade de execução preferida, quando aplicável")
        UUID preferredUnitId
) {
    /** Construtor de compatibilidade que mantém tipoFila como FILA_REGULADA por padrão. */
    public RegisterQueueRequestDTO(UUID patientId, UUID procedureId, ERiskColor riskColor) {
        this(patientId, procedureId, riskColor, EDestino.FILA_REGULADA, null, null);
    }

    /** Construtor de compatibilidade sem solicitacaoId/preferredUnitId. */
    public RegisterQueueRequestDTO(UUID patientId, UUID procedureId, ERiskColor riskColor, EDestino tipoFila) {
        this(patientId, procedureId, riskColor, tipoFila, null, null);
    }
}