package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.model.AgendamentoComDetalhes;
import br.com.morbus.agendamento.domain.port.in.IAgendamentosPacienteUseCase;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

public class AgendamentosPacienteUseCase implements IAgendamentosPacienteUseCase {

    private final IAgendamentoRepository agendamentoRepository;

    public AgendamentosPacienteUseCase(IAgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }

    @Override
    public List<AgendamentoComDetalhes> execute(UUID id,
                                                UUID unitId,
                                                EStatusAgendamento status,
                                                String dateFrom,
                                                String dateTo,
                                                UUID requesterId,
                                                String requesterRole) {

        UUID patientId = resolvePatientId(id, requesterId, requesterRole);

        LocalDateTime parsedDateFrom = parseDate(dateFrom, false);
        LocalDateTime parsedDateTo = parseDate(dateTo, true);

        if (parsedDateFrom.isAfter(parsedDateTo)) {
            throw new IllegalArgumentException("dateFrom deve ser anterior ou igual a dateTo");
        }

        return agendamentoRepository.findByPatientAndStatusAndDate(
                patientId,
                unitId,
                status,
                parsedDateFrom,
                parsedDateTo);
    }

    private UUID resolvePatientId(UUID patientId, UUID requesterId, String requesterRole) {
        if (requesterId == null) {
            return patientId;
        }

        boolean isPaciente = "ROLE_PACIENTE".equals(requesterRole);
        if (!isPaciente) {
            return patientId;
        }

        return requesterId;
    }

    private LocalDateTime parseDate(String value, boolean endOfDay) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                LocalDate date = LocalDate.parse(value);
                return endOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Formato de data invalido. Use ISO-8601 em dateFrom/dateTo.", ex);
            }
        }
    }
}
