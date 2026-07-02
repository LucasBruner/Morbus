package br.com.morbus.regulacao.adapters.out.jpa.quota;

import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.ports.in.dto.ConsultarCotasQuery;
import br.com.morbus.regulacao.ports.out.IQuotaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class QuotaJpaAdapter implements IQuotaRepository {
    private IQuotaJpaRepository quotaJpaRepository;

    public QuotaJpaAdapter(IQuotaJpaRepository quotaJpaRepository) {
        this.quotaJpaRepository = quotaJpaRepository;
    }

    @Override
    public Quota findOrCreate(UUID unitId, UUID procedureId, LocalDate periodStart) {
        return quotaJpaRepository.findByUnitIdAndProcedureIdAndPeriodStart(unitId, procedureId, periodStart)
                .map(QuotaEntity::toDomain)
                .orElseGet(() -> {
                    QuotaEntity entity = QuotaEntity.fromDomain(Quota.bloqueada(unitId, procedureId, periodStart));
                    return quotaJpaRepository.save(entity).toDomain();
                });
    }

    @Override
    public boolean incrementarSeDisponivel(UUID quotaId) {
        return quotaJpaRepository.incrementarSeDisponivel(quotaId) == 1;
    }

    @Override
    public Optional<Quota> buscarPorChave(UUID unitId, UUID procedureId, LocalDate periodStart) {
        return quotaJpaRepository
                .findByUnitIdAndProcedureIdAndPeriodStart(unitId, procedureId, periodStart)
                .map(QuotaEntity::toDomain);
    }

    @Override
    public Quota salvar(Quota cota) {
        QuotaEntity entity = QuotaEntity.fromDomain(cota);
        return quotaJpaRepository.save(entity).toDomain();
    }

    @Override
    public Page<Quota> listar(ConsultarCotasQuery query) {
        Specification<QuotaEntity> specification = (r, cq, cb) -> cb.conjunction();
        if (query.unitId() != null) {
            specification = specification.and((r, cq, cb) -> cb.equal(r.get("unitId"), query.unitId()));
        }

        if (query.procedureId() != null) {
            specification = specification.and((r, cq, cb) -> cb.equal(r.get("procedureId"), query.procedureId()));
        }

        if (query.periodStart() != null) {
            specification = specification.and((r, cq, cb) -> cb.equal(r.get("periodStart"), query.periodStart()));
        }

        Pageable pageable = PageRequest.of(query.page(), query.size());
        return quotaJpaRepository.findAll(specification, pageable).map(QuotaEntity::toDomain);
    }
}
