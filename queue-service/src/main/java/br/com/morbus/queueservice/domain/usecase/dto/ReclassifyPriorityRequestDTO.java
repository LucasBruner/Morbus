package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.enums.ERiskColor;
import jakarta.validation.constraints.NotNull;

public record ReclassifyPriorityRequestDTO(
        @NotNull ERiskColor riskColor
) {}
