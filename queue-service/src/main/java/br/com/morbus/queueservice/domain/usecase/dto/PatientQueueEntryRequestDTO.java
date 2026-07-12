package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.enums.EDestino;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Dados para registrar um paciente na fila. A cor de entrada é sempre AZUL; o grupo legal é detectado automaticamente.")
public record PatientQueueEntryRequestDTO(
        @NotNull
        @Schema(description = "ID do paciente", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID patientId,

        @NotNull
        @Schema(description = "ID do procedimento SIGTAP", example = "7b3c1a2d-9e4f-4a8b-b6d1-1f2e3a4b5c6d")
        UUID procedureId,

        @Schema(description = "Tipo de fila. FILA_REGULADA (padrão) ou FILA_ESPERA.", example = "FILA_REGULADA")
        EDestino tipoFila,

        @Schema(description = "Unidade preferida pelo paciente (opcional)")
        UUID preferredUnitId
) {
}
