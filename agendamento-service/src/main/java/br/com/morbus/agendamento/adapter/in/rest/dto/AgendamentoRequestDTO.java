package br.com.morbus.agendamento.adapter.in.rest.dto;

import br.com.morbus.agendamento.application.command.CriarAgendamentoCommand;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentoRequestDTO(@NotNull(message = "O código do paciente é obrigatório") UUID pacienteId,
                                    @NotNull(message = "O código do procedimento é obrigatório") UUID procedimentoId,
                                    @NotNull(message = "A unidade é obrigatória") UUID unidadeId,
                                    LocalDateTime dataHora) {

    public CriarAgendamentoCommand toCommand() {
        return new CriarAgendamentoCommand(
                pacienteId,
                procedimentoId,
                unidadeId,
                dataHora);
    }
}
