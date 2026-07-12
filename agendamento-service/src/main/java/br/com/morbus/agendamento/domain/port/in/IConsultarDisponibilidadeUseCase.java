package br.com.morbus.agendamento.domain.port.in;

import br.com.morbus.agendamento.domain.model.HealthUnit;
import br.com.morbus.agendamento.domain.model.Provider;
import br.com.morbus.agendamento.domain.model.Schedule;
import br.com.morbus.agendamento.domain.model.Slot;

import java.util.List;
import java.util.UUID;

public interface IConsultarDisponibilidadeUseCase {

    List<SlotItem> execute(UUID procedureId,
                           UUID unitId,
                           String dateFrom,
                           String dateTo);

    record SlotItem(
            Slot slot,
            Schedule schedule,
            HealthUnit unit,
            Provider provider
    ) {}
}