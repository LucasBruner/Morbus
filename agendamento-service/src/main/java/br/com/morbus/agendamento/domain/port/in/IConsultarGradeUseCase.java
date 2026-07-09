package br.com.morbus.agendamento.domain.port.in;

import br.com.morbus.agendamento.domain.model.HealthUnit;
import br.com.morbus.agendamento.domain.model.Provider;
import br.com.morbus.agendamento.domain.model.Schedule;

import java.util.List;
import java.util.UUID;

public interface IConsultarGradeUseCase {

    List<GradeItem> execute(UUID unitId, String week);

    record GradeItem(
            Schedule schedule,
            HealthUnit unit,
            Provider provider
    ) {}
}
