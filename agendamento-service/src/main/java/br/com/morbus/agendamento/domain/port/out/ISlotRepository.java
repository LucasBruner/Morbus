package br.com.morbus.agendamento.domain.port.out;

import br.com.morbus.agendamento.domain.model.Slot;

import java.util.List;

public interface ISlotRepository {

    List<Slot> saveAll(List<Slot> slots);
}
