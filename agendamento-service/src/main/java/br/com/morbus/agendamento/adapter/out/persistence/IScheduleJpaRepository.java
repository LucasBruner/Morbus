package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.enums.EDiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IScheduleJpaRepository extends JpaRepository<ScheduleEntity, UUID> {

    boolean existsByProviderIdAndUnitIdAndDiaDaSemana(UUID providerId,
                                                      UUID unitId,
                                                      EDiaSemana diaDaSemana);
}
