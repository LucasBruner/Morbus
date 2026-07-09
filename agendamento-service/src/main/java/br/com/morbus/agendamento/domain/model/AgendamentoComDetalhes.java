package br.com.morbus.agendamento.domain.model;

public record AgendamentoComDetalhes(Agendamento agendamento,
                                     Slot slot,
                                     Schedule schedule,
                                     HealthUnit unit,
                                     Provider provider) {
}
