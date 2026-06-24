package br.com.morbus.agendamento.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CriarAgendamentoCommand(
        UUID pacienteId,
        UUID procedimentoId,
        UUID unidadeId,
        LocalDateTime dataHora
) {
}
