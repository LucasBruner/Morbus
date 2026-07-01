package br.com.morbus.regulacao.ports.out;

import br.com.morbus.regulacao.domain.model.Quota;

import java.time.LocalDate;
import java.util.UUID;

public interface IQuotaRepository {
    Quota findOrCreate(UUID unitId, UUID procedureId, LocalDate periodStart);
    boolean incrementarSeDisponivel(UUID quotaId); //update
}
