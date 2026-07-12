package br.com.morbus.agendamento.domain.port.in;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.model.AgendamentoComDetalhes;

import java.util.List;
import java.util.UUID;

public interface IAgendamentosPacienteUseCase {

    List<AgendamentoComDetalhes> execute(UUID patientId,
                                         UUID unitId,
                                         EStatusAgendamento status,
                                         String dateFrom,
                                         String dateTo,
                                         UUID requesterId,
                                         String requesterRole);
}