package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.enums.ETurnos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface IScheduleJpaRepository extends JpaRepository<ScheduleEntity, UUID> {

    boolean existsByProviderIdAndDataInicioBetweenAndTurno(UUID providerId,
                                                           LocalDateTime dataInicio,
                                                           LocalDateTime dataFim,
                                                           ETurnos turno);
}
