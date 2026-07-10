package br.com.morbus.regulacao.ports.out;

import br.com.morbus.regulacao.domain.dto.PageResult;
import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.ports.in.dto.ConsultarCotasQuery;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface IQuotaRepository {
    Quota findOrCreate(UUID unitId, UUID procedureId, LocalDate periodStart);
    boolean incrementarSeDisponivel(UUID quotaId);
    Optional<Quota> buscarPorChave(UUID unitId, UUID procedureId, LocalDate periodStart);
    Quota salvar(Quota cota);
    PageResult<Quota> listar(ConsultarCotasQuery query);
}
