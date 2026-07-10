package br.com.morbus.queueservice.domain.usecase;

import br.com.morbus.queueservice.domain.entity.UnitProcedureQuota;
import br.com.morbus.queueservice.domain.exception.QuotaNotFoundException;
import br.com.morbus.queueservice.domain.repository.IUnitProcedureQuotaRepository;

import java.util.UUID;

public class GetQuota {

    private final IUnitProcedureQuotaRepository quotaRepository;

    private GetQuota(IUnitProcedureQuotaRepository quotaRepository) {
        this.quotaRepository = quotaRepository;
    }

    public static GetQuota create(IUnitProcedureQuotaRepository quotaRepository) {
        return new GetQuota(quotaRepository);
    }

    public UnitProcedureQuota execute(UUID unitId, UUID procedureId) {
        return quotaRepository.findByUnitAndProcedure(unitId, procedureId)
                .orElseThrow(() -> new QuotaNotFoundException(
                        "Nenhuma cota configurada para esta unidade e procedimento"));
    }
}
