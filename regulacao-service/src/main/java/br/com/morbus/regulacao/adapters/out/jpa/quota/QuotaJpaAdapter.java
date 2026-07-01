package br.com.morbus.regulacao.adapters.out.jpa.quota;

import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.ports.out.IQuotaRepository;

import java.time.LocalDate;
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
}
