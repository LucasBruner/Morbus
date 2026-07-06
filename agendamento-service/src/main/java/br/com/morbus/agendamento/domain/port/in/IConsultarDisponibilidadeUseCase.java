package br.com.morbus.agendamento.domain.port.in;

import br.com.morbus.agendamento.domain.model.Slot;

import java.util.List;
import java.util.UUID;

public interface IConsultarDisponibilidadeUseCase {

    List<Slot> execute(UUID procedureId,
                       UUID unitId,
                       String dateFrom,
                       String dateTo);
}