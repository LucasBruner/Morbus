package br.com.morbus.agendamento.application.usecase;

import br.com.morbus.agendamento.domain.model.Slot;
import br.com.morbus.agendamento.domain.port.in.IConsultarDisponibilidadeUseCase;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

public class ConsultarDisponibilidadeUseCase implements IConsultarDisponibilidadeUseCase {

    private final ISlotRepository slotRepository;

    public ConsultarDisponibilidadeUseCase(ISlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @Override
    public List<Slot> execute(UUID procedureId,
                              UUID unitId,
                              String dateFrom,
                              String dateTo) {
        LocalDateTime parsedDateFrom = parseDate(dateFrom, false);
        LocalDateTime parsedDateTo = parseDate(dateTo, true);

        if (parsedDateFrom.isAfter(parsedDateTo)) {
            throw new IllegalArgumentException("dateFrom deve ser anterior ou igual a dateTo");
        }

        return slotRepository.findByProcedureAndUnitAndDate(procedureId, unitId, parsedDateFrom, parsedDateTo);
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