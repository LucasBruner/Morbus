package br.com.morbus.queueservice.domain.usecase.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelQueueRequestDTO(
        @NotBlank String motivoCancelamento
) {}