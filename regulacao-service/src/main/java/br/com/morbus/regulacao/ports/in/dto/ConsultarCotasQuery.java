package br.com.morbus.regulacao.ports.in.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ConsultarCotasQuery(UUID unitId,
                                  UUID procedureId,
                                  LocalDate periodStart,
                                  int page,
                                  int size) {
}
