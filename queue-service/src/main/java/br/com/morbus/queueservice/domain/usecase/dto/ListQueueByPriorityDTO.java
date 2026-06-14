package br.com.morbus.queueservice.domain.usecase.dto;

import br.com.morbus.queueservice.domain.enums.EQueueStatus;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ListQueueByPriorityDTO(
        @NotNull(message = "É obrigatório informar o ID do procedimento!")
        UUID procedureId,
        EQueueStatus status,
        ERiskColor riskColor,
        Integer page,
        Integer size
) {
}
