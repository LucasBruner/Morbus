package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import br.com.morbus.agendamento.domain.model.Agendamento;
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
    public List<Agendamento> execute(UUID patientId,
                                     UUID unitId,
                                     EStatusAgendamento status,
                                     String dateFrom,
                                     String dateTo) {

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
