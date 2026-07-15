package br.com.morbus.regulacao.adapters.out.jpa.quota;

import br.com.morbus.regulacao.domain.model.Quota;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "quotas", schema = "regulacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuotaEntity {

    @Id
    private UUID id;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "procedure_id", nullable = false)
    private UUID procedureId;

    @Column(name = "max_per_period", nullable = false)
    private int maxPerPeriod;

    @Column(name = "current_count", nullable = false)
    private int currentCount;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    public Quota toDomain() {
        return new Quota(this.id,
                this.unitId,
                this.procedureId,
                this.maxPerPeriod,
                this.currentCount,
                this.periodStart);
    }

    public static QuotaEntity fromDomain(Quota quota) {
        return new QuotaEntity(quota.getId(),
                quota.getUnitId(),
                quota.getProcedureId(),
                quota.getMaxPerPeriod(),
                quota.getCurrentCount(),
                quota.getPeriodStart());
    }

}
