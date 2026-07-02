package br.com.morbus.regulacao.ports.in.dto;

import java.time.LocalDate;
import java.util.UUID;

public record GerenciarCotaCommand(UUID unitId,
                                   UUID procedureId,
                                   int maxPerPeriod,
                                   LocalDate periodStart) {
}
