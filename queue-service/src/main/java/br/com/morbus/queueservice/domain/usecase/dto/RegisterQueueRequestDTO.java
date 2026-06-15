package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.enums.ERiskColor;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RegisterQueueRequestDTO(
        @NotNull UUID patientId,
        @NotNull UUID procedureId,
        @NotNull ERiskColor riskColor
) {}