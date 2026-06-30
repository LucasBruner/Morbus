package br.com.morbus.agendamento.adapter.in.rest.dto;

import br.com.morbus.agendamento.domain.model.Agendamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentoCreatedResponseDTO(UUID id,
                                            UUID pacienteId,
                                            UUID procedimentoId,
                                            UUID unidadeId,
                                            LocalDateTime dataHora) {

    public static AgendamentoCreatedResponseDTO fromDomain(Agendamento a) {
        return new AgendamentoCreatedResponseDTO(
                a.getId(),
                a.getPacienteId(),
                a.getProcedimentoId(),
                a.getUnidadeId(),
                a.getDataHora()
        );
    }
}
