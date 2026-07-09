package br.com.morbus.agendamento.adapter.in.graphql.dto;

import br.com.morbus.agendamento.domain.model.*;

public record AgendamentoDetalheRequestDTO(
        Agendamento agendamento,
        Slot slot,
        Schedule schedule,
        HealthUnit unit,
        Provider provider
) {}
