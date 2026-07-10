package br.com.morbus.agendamento.adapter.in.rest.dto;

import br.com.morbus.agendamento.application.command.ConfirmarAgendamentoResult;
import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConfirmarAgendamentoResponseDTO(
        UUID id,
        EStatusAgendamento status,
        LocalDateTime confirmedAt,
        LocalDateTime slotDateTime,
        String unitName,
        String unitAddress
) {

    public static ConfirmarAgendamentoResponseDTO fromResult(ConfirmarAgendamentoResult result) {
        return new ConfirmarAgendamentoResponseDTO(
                result.id(),
                result.status(),
                result.confirmedAt(),
                result.slotDate(),
                result.unitName(),
                result.unitAddress()
        );
    }
}
