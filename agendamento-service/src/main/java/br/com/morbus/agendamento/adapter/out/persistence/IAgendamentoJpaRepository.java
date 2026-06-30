package br.com.morbus.agendamento.adapter.out.persistence;

import br.com.morbus.agendamento.domain.enums.EStatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface IAgendamentoJpaRepository extends JpaRepository<AgendamentoEntity, UUID> {

    List<AgendamentoEntity> findByStatusAndExpiresAtBefore(EStatusAgendamento status, LocalDateTime threshold);
}
