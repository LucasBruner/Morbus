package br.com.morbus.agendamento.adapter.out.persistence;

public record AgendamentoListProjection(AgendamentoEntity agendamento,
                                        SlotEntity slot,
                                        ScheduleEntity schedule,
                                        HealthUnitEntity unit,
                                        ProviderEntity provider) {
}
